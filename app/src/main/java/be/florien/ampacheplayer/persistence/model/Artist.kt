package be.florien.ampacheplayer.persistence.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import be.florien.ampacheplayer.api.model.AmpacheArtist

/**
 * Database structure that represents to artists
 */
@Entity
open class Artist {
    @field:PrimaryKey
    var id: Long = 0
    var name: String = ""
    @Ignore
    var tag: MutableList<Tag> = mutableListOf()
    var preciserating: Int = 0
    var rating: Double = 0.0

    constructor() : super()

    constructor(fromServer: AmpacheArtist) : super() {
        id = fromServer.id
        name = fromServer.name
        tag.addAll(fromServer.tag.map(::Tag))
        preciserating = fromServer.preciserating
        rating = fromServer.rating
    }
}
