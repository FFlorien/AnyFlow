package be.florien.ampacheplayer.persistence.local.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import be.florien.ampacheplayer.persistence.server.model.AmpacheTag
import be.florien.ampacheplayer.persistence.server.model.AmpacheTagName

/**
 * Database structure that represents to tags
 */
@Entity
open class Tag {
    @field:PrimaryKey
    var id: Long = 0
    var name: String = ""

    constructor() : super()

    constructor(fromServer: AmpacheTag) : super() {
        id = fromServer.id
        name = fromServer.name
    }

    constructor(fromServer: AmpacheTagName) : super() {
        id = fromServer.id
        name = fromServer.value
    }
}

@ForeignKey(entity = Album::class, parentColumns = ["id"], childColumns = ["albumId"])
class AlbumTag : Tag() {
    var albumId: Long = 0
}

@ForeignKey(entity = Artist::class, parentColumns = ["id"], childColumns = ["artistId"])
class ArtistTag : Tag() {
    var artistId: Long = 0
}

@ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"])
class PlaylistTag : Tag() {
    var playlistId: Long = 0
}

@ForeignKey(entity = Song::class, parentColumns = ["id"], childColumns = ["songId"])
class SongTag : Tag() {
    var songId: Long = 0
}