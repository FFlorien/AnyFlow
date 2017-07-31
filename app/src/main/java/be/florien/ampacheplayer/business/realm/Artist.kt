package be.florien.ampacheplayer.business.realm

import be.florien.ampacheplayer.business.ampache.AmpacheArtist
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Database structure that represents to artists
 */
open class Artist : RealmObject{
    @PrimaryKey
    var id: Long = 0
    var name: String = ""
    var tag: RealmList<Tag> = RealmList()
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
