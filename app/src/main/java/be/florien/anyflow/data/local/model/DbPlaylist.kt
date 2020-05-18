package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database structure that represents to playlist
 */

@Entity(tableName = "Playlist")
data class DbPlaylist(
        @PrimaryKey
        val id: Long,
        val name: String,
        val owner: String)