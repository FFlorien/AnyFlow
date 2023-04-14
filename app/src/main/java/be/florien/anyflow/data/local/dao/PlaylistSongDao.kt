package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.data.local.model.DbPlaylistSongs
import be.florien.anyflow.data.local.model.DbSongDisplay

@Dao
abstract class PlaylistSongDao : BaseDao<DbPlaylistSongs>() {

    @Transaction
    @Query("SELECT song.id AS id, song.title AS title, artist.name AS artistName, album.name AS albumName, album.id AS albumId, song.time AS time FROM song JOIN artist ON song.artistId = artist.id JOIN album ON song.albumId = album.id JOIN playlistsongs ON song.id = playlistsongs.songId WHERE playlistsongs.playlistId = :playlistId ORDER BY playlistsongs.`order`")
    abstract fun songsFromPlaylist(playlistId: Long): DataSource.Factory<Int, DbSongDisplay>

    @Query("SELECT count(*) FROM playlistsongs WHERE playlistId = :playlistId AND songId = :songId")
    abstract suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Int

    @Query("SELECT max(`order`) FROM playlistsongs WHERE playlistId = :playlistId")
    abstract suspend fun playlistLastOrder(playlistId: Long): Int?

    @Query("DELETE FROM playlistsongs WHERE playlistId = :playlistId")
    abstract suspend fun deleteSongsFromPlaylist(playlistId: Long)
}