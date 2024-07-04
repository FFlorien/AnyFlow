package be.florien.anyflow.feature.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.local.model.DownloadProgressState
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.logging.eLog
import be.florien.anyflow.injection.ServerScope
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named

@ServerScope
class DownloadManager @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val urlRepository: UrlRepository,
    @Named("authenticated")
    private val okHttpClient: OkHttpClient,
    context: Context
) {
    private val contentResolver = context.contentResolver
    private val downloadProgressMap = mutableMapOf<Long, LiveData<DownloadProgressState>>()
    private var _currentDownloads: List<Long>? = null
    private var currentDownloads: List<Long>
        get() = _currentDownloads ?: emptyList()
        set(value) {
            _currentDownloads = value
        }
    private var currentDownload = -1L

    //todo should we care about cancellation ? (Yes, yes we should)
    private val downloadScope = CoroutineScope(Dispatchers.IO)

    init {
        nextDownload()
    }

    fun queueDownload(typeId: Long, filterType: Filter.FilterType, secondId: Int? = null) {
        downloadScope.launch(Dispatchers.IO) {
            downloadRepository.queueDownload(typeId, filterType, secondId)
            nextDownload()
        }
    }

    fun getDownloadState(id: Long, filterType: Filter.FilterType, secondId: Int? = null): LiveData<DownloadProgressState> =
        if (filterType == Filter.FilterType.SONG_IS) {
            val livedata = downloadProgressMap[id] ?: MutableLiveData()
            downloadProgressMap[id] = livedata
            if (currentDownloads.contains(id) && currentDownload != id) {
                (livedata as MutableLiveData).value = DownloadProgressState(1, 0, 1)
            }
            livedata
        } else {
            downloadRepository.getProgressForDownloadCandidate(id, filterType, secondId)
        }

    private fun nextDownload() {
        downloadScope.launch(Dispatchers.IO) {
            currentDownloads = downloadRepository.getDownloadList()
            if (currentDownloads.isEmpty() || currentDownload >= 0) {
                return@launch
            }

            val currentDownload = currentDownloads[0]
            downloadSong(currentDownload)

        }
    }

    private fun downloadSong(songId: Long) {
        downloadScope.launch(Dispatchers.IO) {
            currentDownload = songId
            val songInfo = downloadRepository.getSongSync(songId)
            val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val newSongDetails = getNewSongDetails(songInfo)
            val newSongUri = contentResolver.insert(audioCollection, newSongDetails)
                ?: return@launch

            val songUrl = urlRepository.getSongUrl(songInfo.id)
            okHttpClient
                .newCall(Request.Builder().get().url(songUrl).build())
                .execute()
                .body
                ?.byteStream()
                ?.use { iStream ->
                    contentResolver.openOutputStream(newSongUri)?.use { oStream ->
                        try {
                            var bytesCopied = 0
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var bytes = iStream.read(buffer)
                            while (bytes >= 0) {
                                oStream.write(buffer, 0, bytes)
                                bytesCopied += bytes
                                withContext(Dispatchers.Main) {
                                    (downloadProgressMap[songInfo.id] as? MutableLiveData)?.value =
                                        DownloadProgressState(
                                            songInfo.size,
                                            bytesCopied,
                                            songInfo.size
                                        )
                                }
                                bytes = iStream.read(buffer)
                            }
                            downloadRepository.concludeDownload(
                                songInfo.id,
                                newSongUri.toString()
                            )
                        } catch (exception: Exception) {
                            eLog(exception, "Could not download a song")
                            contentResolver.delete(newSongUri, null, null)
                        }
                    }
                }
            currentDownloads = downloadRepository.getDownloadList()
            currentDownload = -1L
            nextDownload()
        }
    }

    private fun getNewSongDetails(songInfo: SongInfo): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, songInfo.title)
            put(MediaStore.Audio.Media.ARTIST, songInfo.artistName)
            put(MediaStore.Audio.Media.ARTIST_ID, songInfo.artistId)
            put(MediaStore.Audio.Media.ALBUM, songInfo.albumName)
            put(MediaStore.Audio.Media.ALBUM_ID, songInfo.albumId)
            put(MediaStore.Audio.Media.TRACK, songInfo.track)
            put(MediaStore.Audio.Media.DURATION, songInfo.time)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(MediaStore.Audio.Media.ALBUM_ARTIST, songInfo.albumArtistName)
                put(MediaStore.Audio.Media.GENRE, songInfo.genreNames.first())
                if (songInfo.year != 0) {
                    put(MediaStore.Audio.Media.YEAR, songInfo.year)
                }
            }
        }
    }

    suspend fun removeDownload(id: Long?) {
        if (id == null) {
            return
        }

        val songInfo = downloadRepository.getSongSync(id)
        val local = songInfo.local

        if (local.isNullOrBlank()) {
            return
        }

        contentResolver.delete(local.toUri(), null, null)
        downloadRepository.concludeDownload(id, null)
    }
}