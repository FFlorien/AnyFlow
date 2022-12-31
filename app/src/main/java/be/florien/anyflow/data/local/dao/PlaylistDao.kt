package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbPlaylist
import be.florien.anyflow.data.local.model.DbPlaylistWithCount
import be.florien.anyflow.data.local.model.DbPlaylistWithSongs

@Dao
abstract class PlaylistDao : BaseDao<DbPlaylist>() {
    @Query("SELECT * FROM playlist")
    abstract fun getPlaylists(): LiveData<List<DbPlaylist>>

    @Transaction
    @Query("SELECT * FROM playlist")
    abstract fun getPlaylistsWithSongs(): LiveData<List<DbPlaylistWithSongs>>

    @Query("SELECT DISTINCT id, name, (SELECT COUNT(*) FROM playlistSongs WHERE playlistId = id) as songCount FROM playlist ORDER BY name COLLATE UNICODE")
    abstract fun orderByName(): DataSource.Factory<Int, DbPlaylistWithCount>

    @Query("SELECT DISTINCT id, name, (SELECT COUNT(*) FROM playlistSongs WHERE playlistId = id) as songCount FROM playlist WHERE playlist.name LIKE :search ORDER BY name COLLATE UNICODE")
    abstract fun orderByNameSearched(search: String): DataSource.Factory<Int, DbPlaylistWithCount>

    @Query("SELECT DISTINCT id, name, (SELECT COUNT(*) FROM playlistSongs WHERE playlistId = id) as songCount FROM playlist WHERE playlist.name LIKE :search ORDER BY name COLLATE UNICODE")
    abstract suspend fun orderByNameSearchedList(search: String): List<DbPlaylistWithCount>

    @RawQuery(observedEntities = [DbPlaylist::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbPlaylistWithCount>
}