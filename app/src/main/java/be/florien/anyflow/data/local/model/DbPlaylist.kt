package be.florien.anyflow.data.local.model

import androidx.room.*

/**
 * Database structure that represents to playlist
 */

@Entity(tableName = "Playlist")
data class DbPlaylist(
    @PrimaryKey
    val id: Long,
    val name: String,
    val owner: String
)

@Entity(
    tableName = "PlaylistSongs",
    primaryKeys = ["order", "songId", "playlistId"]
)
data class DbPlaylistSongs(
    val order: Int,
    @ColumnInfo(index = true)
    val songId: Long,
    @ColumnInfo(index = true)
    val playlistId: Long
)

data class DbPlaylistWithCount(
    val id: Long,
    val name: String,
    val songCount: Int
)

data class DbPlaylistWithCountAndPresence(
    val id: Long,
    val name: String,
    val songCount: Int,
    val presence: Int
)
