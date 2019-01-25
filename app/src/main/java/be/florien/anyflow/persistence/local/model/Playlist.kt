package be.florien.anyflow.persistence.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import be.florien.anyflow.persistence.server.model.AmpachePlayList

/**
 * Database structure that represents to playlist
 */

@Entity
open class Playlist(
        @PrimaryKey
        val id: Long,
        val name: String,
        val owner: String) {

    constructor(fromServer: AmpachePlayList) : this(
            fromServer.id,
            fromServer.name,
            fromServer.owner)
}