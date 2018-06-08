package be.florien.ampacheplayer.persistence.model

import android.arch.persistence.room.Entity
import be.florien.ampacheplayer.api.model.AmpacheArtist
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Database structure that represents to artists
 */
@Entity
open class Artist {
    @PrimaryKey
    var id: Long = 0
    var name: String = ""
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
