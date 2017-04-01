package be.florien.ampacheplayer.model.realm

import be.florien.ampacheplayer.model.server.AlbumName
import be.florien.ampacheplayer.model.server.AlbumServer
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Data structures that relates to album
 */
open class Album : RealmObject {
    @field:PrimaryKey
    var id: Long = 0
    var name: String = ""
    var artist: Artist = Artist()
    var year: Int = 0
    var tracks: Int = 0
    var disk: Int = 0
    var tag: RealmList<Tag> = RealmList()
    var art: String = ""
    var preciserating: Int = 0
    var rating: Double = 0.0

    constructor() : super()

    constructor(fromServer: AlbumServer) : super() {
        id = fromServer.id
        name = fromServer.name
        artist = Artist(fromServer.artist)
        year = fromServer.year
        tracks = fromServer.tracks
        disk = fromServer.disk
        tag.addAll(fromServer.tag.map(::Tag))
        art = fromServer.art
        preciserating = fromServer.preciserating
        rating = fromServer.rating
    }

    constructor(fromServer: AlbumName) : super() {
        id = fromServer.id
        name = fromServer.name
    }
}