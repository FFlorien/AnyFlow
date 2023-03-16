package be.florien.anyflow.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ServerScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

@ServerScope
class DownloadManager @Inject constructor(
    private val dataRepository: DataRepository,
    context: Context
) {
    private val contentResolver = context.contentResolver
    private val downloadMap = mutableMapOf<Long, LiveData<Int>>()
    private val downloadScope =
        CoroutineScope(Dispatchers.IO)//todo should we care about cancellation ? (Yes, yes we should)

    /*todo list :
        - saving download list in a db
        - retrieving different level of download (artist, album, album artist, playlist, genre)
        - only 2 or 3 download in parallel
        - check for download state
        - do not download twice
        - only one public method ? (download id + FilterType.Song ?)
        - contentProvider.bulkInsert ?
        - hasDownloadStarted ?
     */

    fun download(songInfo: SongInfo) {
        val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val newSongDetails = getNewSongDetails(songInfo)

        val newSongUri =
            contentResolver.insert(audioCollection, newSongDetails) ?: return

        val liveData = MutableLiveData(0)

        downloadScope.launch(Dispatchers.IO) {
            val songUrl = dataRepository.getSongUrl(songInfo.id)
            URL(songUrl).openStream().use { iStream ->
                contentResolver.openOutputStream(newSongUri)?.use { oStream ->
                    try {
                        var bytesCopied = 0
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytes = iStream.read(buffer)
                        while (bytes >= 0) {
                            oStream.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            withContext(Dispatchers.Main) {
                                liveData.value = bytesCopied * 100 / songInfo.size
                            }
                            bytes = iStream.read(buffer)
                        }
                        dataRepository.updateSongLocalUri(songInfo.id, newSongUri.toString())
                    } catch (exception: Exception) {
                        eLog(exception, "Could not download a song")
                        contentResolver.delete(newSongUri, null, null)
                    }
                }
            }
            downloadMap.remove(songInfo.id)
        }
        downloadMap[songInfo.id] = liveData
    }

    fun batchDownload(typeId: Long, filterType: Filter.FilterType) {
        downloadScope.launch(Dispatchers.IO) {
            val songs = dataRepository.getSongsSearchedList(listOf(Filter(filterType, typeId, "")), "") {
                it.toViewSongInfo()
            }
            songs.forEach(::download)
        }
    }

    fun getDownloadState(songInfo: SongInfo) = downloadMap[songInfo.id]

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

        val songInfo = dataRepository.getSongSync(id)
        val local = songInfo.local

        if (local.isNullOrBlank()) {
            return
        }

        contentResolver.delete(local.toUri(), null, null)
        dataRepository.updateSongLocalUri(id, null)
    }
}