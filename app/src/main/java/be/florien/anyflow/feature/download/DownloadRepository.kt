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

    suspend fun queueDownload(id: Long, type: Filter.FilterType, secondId: Int?) {
        val filter = if (type == Filter.FilterType.DISK_IS) {
            Filter(Filter.FilterType.ALBUM_IS, id, "", listOf(Filter(type, secondId, " ")))
        } else {
            Filter(type, id, "")
        }
        libraryDatabase
            .getDownloadDao()
            .rawQueryInsert(
                queryComposer
                    .getQueryForDownload(listOfNotNull(filter))
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
        type: Filter.FilterType,
        secondId: Int? = null
    ): LiveData<DownloadProgressState> {
        val filter = if (type == Filter.FilterType.DISK_IS) {
            Filter(Filter.FilterType.ALBUM_IS, id, "", listOf(Filter(type, secondId, "")))
        } else {
            Filter(type, id, "")
        }
        return libraryDatabase
            .getDownloadDao()
            .rawQueryProgress(
                queryComposer
                    .getQueryForDownloadProgress(listOfNotNull(filter))
            )
    }

    suspend fun concludeDownload(songId: Long, uri: String?) {
        libraryDatabase.withTransaction {
            libraryDatabase.getSongDao().updateWithLocalUri(songId, uri)
            libraryDatabase.getDownloadDao().delete(DbDownload(songId))
        }
    }
}