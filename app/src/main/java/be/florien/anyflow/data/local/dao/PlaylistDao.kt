package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT DISTINCT id, name, COUNT(*) as songCount FROM playlist JOIN playlistsongs ON playlistId = id GROUP BY id ORDER BY name COLLATE UNICODE")
    abstract fun orderByName(): DataSource.Factory<Int, DbPlaylistWithCount>

    @Query("SELECT DISTINCT id, name, COUNT(*) as songCount FROM playlist JOIN playlistsongs ON playlistId = id WHERE playlist.name LIKE :filter GROUP BY id ORDER BY name COLLATE UNICODE")
    abstract fun orderByNameFiltered(filter: String): DataSource.Factory<Int, DbPlaylistWithCount>

    @Query("SELECT DISTINCT id, name, COUNT(*) as songCount FROM playlist JOIN playlistsongs ON playlistId = id WHERE playlist.name LIKE :filter GROUP BY id ORDER BY name COLLATE UNICODE")
    abstract suspend fun orderByNameFilteredList(filter: String): List<DbPlaylistWithCount>
}