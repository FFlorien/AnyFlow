package be.florien.anyflow.feature.download

import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.QueryComposer
import be.florien.anyflow.data.local.model.DbDownload
import be.florien.anyflow.data.local.model.DownloadProgressState
import be.florien.anyflow.data.toViewSongInfo
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.SongInfo
import javax.inject.Inject

class DownloadRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase
) {
    private val queryComposer = QueryComposer()

    suspend fun getSongSync(id: Long): SongInfo =
        libraryDatabase.getSongDao().findByIdSync(id).toViewSongInfo()

    suspend fun queueDownload(id: Long, type: Filter.FilterType) {
        libraryDatabase
            .getDownloadDao()
            .rawQueryInsert(
                queryComposer
                    .getQueryForDownload(listOfNotNull(Filter(type, id, "")))
            )
    }

    suspend fun getDownloadList() = libraryDatabase
        .getDownloadDao()
        .list()
        .map { download ->
            download.songId
        }

    fun getProgressForDownloadCandidate(
        id: Long,
        type: Filter.FilterType
    ): LiveData<DownloadProgressState> =
        libraryDatabase
            .getDownloadDao()
            .rawQueryProgress(
                queryComposer
                    .getQueryForDownloadProgress(listOfNotNull(Filter(type, id, "")))
            )

    suspend fun concludeDownload(songId: Long, uri: String?) {
        libraryDatabase.withTransaction {
            libraryDatabase.getSongDao().updateWithLocalUri(songId, uri)
            libraryDatabase.getDownloadDao().delete(DbDownload(songId))
        }
    }
}