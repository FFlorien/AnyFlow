package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.data.local.model.DbPlaylist
import be.florien.anyflow.data.local.model.DbPlaylistWithSongs

@Dao
interface PlaylistDao : BaseDao<DbPlaylist> {
    @Query("SELECT * FROM playlist")
    fun getPlaylists(): LiveData<List<DbPlaylist>>

    @Transaction
    @Query("SELECT * FROM playlist")
    fun getPlaylistsWithSongs(): LiveData<List<DbPlaylistWithSongs>>

    @Query("SELECT * FROM playlist ORDER BY name COLLATE UNICODE")
    fun orderByName(): DataSource.Factory<Int, DbPlaylist>

    @Query("SELECT * FROM playlist WHERE playlist.name LIKE :filter ORDER BY name COLLATE UNICODE")
    fun orderByNameFiltered(filter: String): DataSource.Factory<Int, DbPlaylist>

    @Query("SELECT * FROM playlist WHERE playlist.name LIKE :filter ORDER BY name COLLATE UNICODE")
    suspend fun orderByNameFilteredList(filter: String): List<DbPlaylist>
}