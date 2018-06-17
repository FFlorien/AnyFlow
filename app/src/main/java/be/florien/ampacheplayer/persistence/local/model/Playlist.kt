package be.florien.ampacheplayer.persistence.local.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import be.florien.ampacheplayer.persistence.server.model.AmpachePlayList

/**
 * Database structure that represents to playlist
 */

@Entity
open class Playlist {
    @PrimaryKey
    var id: Long = 0
    var name: String = ""
    var owner: String = ""
    @Ignore
    var tag: MutableList<Tag> = mutableListOf()

    constructor() : super()

    constructor(fromServer: AmpachePlayList) : super(){
        id = fromServer.id
        name = fromServer.name
        owner = fromServer.owner
        tag.addAll(fromServer.tag.map(::Tag))
    }
}