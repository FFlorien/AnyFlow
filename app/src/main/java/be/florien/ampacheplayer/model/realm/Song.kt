package be.florien.ampacheplayer.model.realm

import be.florien.ampacheplayer.model.server.SongServer
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Data structures that relates to songs
 */
open class Song : RealmObject {
    @field:PrimaryKey
    var id: Long = 0
    var song: String = ""
    var title: String = ""
    var name: String = ""
    var artist: Artist = Artist()
    var album: Album = Album()
    var albumartist: Artist = Artist()
    var tag: RealmList<Tag> = RealmList()
    var filename: String = ""
    var track: Int = 0
    var time: Int = 0
    var year: Int = 0
    var bitrate: Int = 0
    var rate: Int = 0
    var url: String = ""
    var art: String = ""
    var preciserating: Int = 0
    var rating: Int = 0
    var averagerating: Int = 0
    var composer: String = ""
    var comment: String = ""
    var publisher: String = ""
    var language: String = ""
    var genre: String = ""

    constructor() : super()

    constructor(fromServer: SongServer) : super() {
        id = fromServer.id
        song = fromServer.song
        title = fromServer.title
        name = fromServer.name
        artist = Artist(fromServer.artist)
        album = Album(fromServer.album)
        albumartist = Artist(fromServer.albumartist)
        tag.addAll(fromServer.tag.map(::Tag))
        filename = fromServer.filename
        track = fromServer.track
        time = fromServer.time
        year = fromServer.year
        bitrate = fromServer.bitrate
        rate = fromServer.rate
        url = fromServer.url
        art = fromServer.art
        preciserating = fromServer.preciserating
        rating = fromServer.rating
        averagerating = fromServer.averagerating
        composer = fromServer.composer
        comment = fromServer.comment
        publisher = fromServer.publisher
        language = fromServer.language
        genre = fromServer.genre
    }
}
