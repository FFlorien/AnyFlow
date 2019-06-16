package be.florien.anyflow.local

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.persistence.local.model.SongDisplay
import be.florien.anyflow.persistence.server.AmpacheConnection
import java.io.File
import javax.inject.Inject


/**
 * Class hiding the complexity of handling download with the download manager
 */
class DownloadHelper @Inject constructor(private val libraryDatabase: LibraryDatabase,
                                         private val ampacheConnection: AmpacheConnection,
                                         context: Context) {

    private val downloadManager: DownloadManager = ContextCompat.getSystemService(context, DownloadManager::class.java)
            ?: throw IllegalStateException("Can't access the downloadManager")

    fun addSongDownload(song: SongDisplay) {
        if (isFileExisting(song)) {
            return
        }

        val uri = Uri.parse(ampacheConnection.getSongUrl(song.url))
        val request = DownloadManager.Request(uri)
        request.setTitle(song.title)
        request.setDescription("Downloading")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setVisibleInDownloadsUi(false)
        val filename = getFileName(song)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, filename)

        downloadManager.enqueue(request)
        libraryDatabase.updateWithLocalFilename(song.id, getFile(song).absolutePath).subscribe()
    }

    fun addAlbumDownload(albumId: Long) {
        libraryDatabase.getSongsForAlbum(albumId)
                .doOnSuccess { list ->
                    list.forEach {song ->
                        addSongDownload(song)
                    }
                }
                .subscribe()

    }

    fun addArtistDownload(artistId: Long) {
        libraryDatabase.getSongsForArtist(artistId)
                .doOnSuccess { list ->
                    list.forEach {song ->
                        addSongDownload(song)
                    }
                }
                .subscribe()

    }

    fun addGenreDownload(genre: String) {
        libraryDatabase.getSongsForGenre(genre)
                .doOnSuccess { list ->
                    list.forEach {song ->
                        addSongDownload(song)
                    }
                }
                .subscribe()

    }

    fun isFileExisting(song: SongDisplay) = getFile(song).exists()

    private fun getFile(song: SongDisplay) =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), getFileName(song))

    private fun getFileName(song: SongDisplay) =
            song.albumArtistName.toValidFileName() + "/" + song.albumName.toValidFileName() + "/" + song.filename.substringAfterLast("/").toValidFileName()

    private fun String.toValidFileName() = replace(Regex("[^a-zA-Z0-9 \\.\\-]"), "_")

    companion object {
        const val REQUEST_WRITING = 5
    }
}