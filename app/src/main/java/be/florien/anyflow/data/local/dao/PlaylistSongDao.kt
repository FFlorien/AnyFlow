package be.florien.anyflow.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbPlaylistSongs

@Dao
interface PlaylistSongDao : BaseDao<DbPlaylistSongs> {
    @Query("SELECT count(*) FROM playlistsongs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Int
}