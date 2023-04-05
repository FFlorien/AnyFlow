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

    @Query("SELECT playlistsongs.playlistId FROM playlistsongs WHERE playlistsongs.songId = :songId")
    abstract suspend fun getPlaylistsWithCountAndSongPresence(songId: Long): List<Long>

    @Transaction
    @Query("SELECT * FROM playlist")
    abstract fun getPlaylistsWithSongs(): LiveData<List<DbPlaylistWithSongs>>

    @RawQuery(observedEntities = [DbPlaylist::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbPlaylistWithCount>

    @RawQuery(observedEntities = [DbPlaylist::class])
    abstract suspend fun rawQueryListDisplay(query: SupportSQLiteQuery): List<DbPlaylistWithCount>
}