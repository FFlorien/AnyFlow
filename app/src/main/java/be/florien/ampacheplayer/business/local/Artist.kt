package be.florien.ampacheplayer.business.local

import be.florien.ampacheplayer.business.realm.RealmArtist
import be.florien.ampacheplayer.business.ampache.AmpacheArtist

/**
 * Database structure that represents to artists
 */
data class Artist(
        var id: Long = 0,
        var name: String = "",
        var tag: List<Tag> = emptyList(),
        var preciseRating: Int = 0,
        var rating: Double = 0.0) {

    constructor(fromServer: AmpacheArtist) : this(
            id = fromServer.id,
            name = fromServer.name,
            tag = fromServer.tag.map(::Tag),
            preciseRating = fromServer.preciserating,
            rating = fromServer.rating)

    constructor(fromRealm: RealmArtist) : this(
            id = fromRealm.id,
            name = fromRealm.name,
            tag = fromRealm.tag.map(::Tag),
            preciseRating = fromRealm.preciserating,
            rating = fromRealm.rating)
}
