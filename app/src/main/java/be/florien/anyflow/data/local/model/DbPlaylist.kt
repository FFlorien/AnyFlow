package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import be.florien.anyflow.data.server.model.AmpachePlayList

/**
 * Database structure that represents to playlist
 */

@Entity(tableName = "Playlist")
open class DbPlaylist(
        @PrimaryKey
        val id: Long,
        val name: String,
        val owner: String)