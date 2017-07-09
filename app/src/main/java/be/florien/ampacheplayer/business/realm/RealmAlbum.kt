package be.florien.ampacheplayer.business.realm

import be.florien.ampacheplayer.business.ampache.AmpacheAlbum
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Database structure that represents to album
 */
open class RealmAlbum : RealmObject {
    @field:PrimaryKey
    var id: Long = 0
    var name: String = ""
    var artistName: String = ""
    var artistId: Long = -1
    var year: Int = 0
    var tracks: Int = 0
    var disk: Int = 0
    var tag: RealmList<RealmTag> = RealmList()
    var art: String = ""
    var preciserating: Int = 0
    var rating: Double = 0.0

    constructor() : super()

    constructor(fromServer: AmpacheAlbum) : super() {
        id = fromServer.id
        name = fromServer.name
        artistName = fromServer.artist.name
        artistId = fromServer.artist.id
        year = fromServer.year
        tracks = fromServer.tracks
        disk = fromServer.disk
        tag.addAll(fromServer.tag.map(::RealmTag))
        art = fromServer.art
        preciserating = fromServer.preciserating
        rating = fromServer.rating
    }
}