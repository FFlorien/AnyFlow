package be.florien.anyflow.data

import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.view.SongInfo
import javax.inject.Inject

class DownloadRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase
) {
    suspend fun getSongSync(id: Long): SongInfo =
        libraryDatabase.getSongDao().findByIdSync(id).toViewSongInfo()

    suspend fun updateSongLocalUri(songId: Long, uri: String?) {
        libraryDatabase.getSongDao().updateWithLocalUri(songId, uri)
    }
}