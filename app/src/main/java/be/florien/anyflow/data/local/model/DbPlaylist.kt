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
        val owner: String)

@Entity(tableName = "PlaylistSongs",
        primaryKeys = ["songId", "playlistId"])
data class DbPlaylistSongs(
        val songId: Long,
        val playlistId: Long)

data class DbPlaylistWithSongs(
        @Embedded val playlist: DbPlaylist,
        @Relation(
                parentColumn = "id",
                entityColumn = "id",
                associateBy = Junction(DbPlaylistSongs::class, parentColumn = "playlistId", entityColumn = "songId")
        )
        val songs: List<DbSong>
)