package be.florien.ampacheplayer.model.realm

import be.florien.ampacheplayer.model.server.AlbumArtist
import be.florien.ampacheplayer.model.server.ArtistServer
import be.florien.ampacheplayer.model.server.ArtistName
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Data structures that relates to artists
 */
open class Artist : RealmObject{
    @PrimaryKey
    var id: Long = 0
    var name: String = ""
    var tag: RealmList<Tag> = RealmList()
    var preciserating: Int = 0
    var rating: Double = 0.0

    constructor() : super()

    constructor(fromServer: ArtistServer) : super() {
        id = fromServer.id
        name = fromServer.name
        tag.addAll(fromServer.tag.map(::Tag))
        preciserating = fromServer.preciserating
        rating = fromServer.rating
    }

    constructor(fromServer: ArtistName) : super() {
        id = fromServer.id
        name = fromServer.name
    }

    constructor(fromServer: AlbumArtist) : super() {
        id = fromServer.id
        name = fromServer.name
    }
}
