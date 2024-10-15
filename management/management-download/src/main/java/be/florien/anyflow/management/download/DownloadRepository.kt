package be.florien.anyflow.management.download

import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.tags.local.LibraryDatabase
import be.florien.anyflow.tags.local.query.QueryComposer
import be.florien.anyflow.tags.local.model.DbDownload
import be.florien.anyflow.tags.local.model.DownloadProgressState
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.tags.model.SongInfo
import be.florien.anyflow.tags.toQueryFilters
import be.florien.anyflow.tags.toViewSongInfo
import javax.inject.Inject

class DownloadRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase
) {
    private val queryComposer = QueryComposer()

    suspend fun getSongSync(id: Long): SongInfo =
        libraryDatabase.getSongDao().songById(id).toViewSongInfo()

    suspend fun queueDownload(id: Long, type: Filter.FilterType, secondId: Int?) {
        val filter = if (type == Filter.FilterType.DISK_IS) {
            Filter(
                Filter.FilterType.ALBUM_IS,
                id,
                "",
                listOf(Filter(type, secondId, " "))
            )
        } else {
            Filter(type, id, "")
        }
        libraryDatabase
            .getDownloadDao()
            .rawQueryInsert(queryComposer.getQueryForDownload(listOfNotNull(filter).toQueryFilters()))
    }

    suspend fun getDownloadList() = libraryDatabase
        .getDownloadDao()
        .allList()
        .map { download ->
            download.mediaId
        }

    fun getProgressForDownloadCandidate(
        id: Long,
        type: Filter.FilterType,
        secondId: Int? = null
    ): LiveData<DownloadProgressState> {
        val filter = if (type == Filter.FilterType.DISK_IS) {
            Filter(
                Filter.FilterType.ALBUM_IS,
                id,
                "",
                listOf(Filter(type, secondId, ""))
            )
        } else {
            Filter(type, id, "")
        }
        return libraryDatabase
            .getDownloadDao()
            .rawQueryProgress(
                queryComposer
                    .getQueryForDownloadProgress(listOfNotNull(filter).toQueryFilters())
            )
    }

    suspend fun concludeDownload(songId: Long, uri: String?) {
        libraryDatabase.withTransaction {
            libraryDatabase.getSongDao().updateWithLocalUri(songId, uri)
            libraryDatabase.getDownloadDao().delete(DbDownload(songId, SONG_MEDIA_TYPE))
        }
    }
}