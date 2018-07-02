package be.florien.ampacheplayer.persistence.local.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import be.florien.ampacheplayer.persistence.server.model.AmpachePlayList

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