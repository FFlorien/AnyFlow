package be.florien.ampacheplayer.persistence.model

import be.florien.ampacheplayer.api.model.AmpacheSong
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

/**
 * Database structure that represents to accounts
 */
@RealmClass
open class Song : RealmObject {
    @field:PrimaryKey
    var id: Long = 0
    var song: String = ""
    var title: String = ""
    var name: String = ""
    var artistName: String = ""
    @field:Index
    var artistId: Long = -1
    var albumName: String = ""
    @field:Index
    var albumId: Long = -1
    var albumArtistName: String = ""
    @field:Index
    var albumArtistId: Long = -1
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
    var averagerating: Double = 0.0
    var composer: String = ""
    var comment: String = ""
    var publisher: String = ""
    var language: String = ""
    @field:Index
    var genre: String = ""

    constructor() : super()

    constructor(fromServer: AmpacheSong) : super() {
        id = fromServer.id
        song = fromServer.song
        title = fromServer.title
        name = fromServer.name
        artistName = fromServer.artist.name
        artistId = fromServer.artist.id
        albumName = fromServer.album.name
        albumId = fromServer.album.id
        albumArtistName = fromServer.albumartist.name
        albumArtistId = fromServer.albumartist.id
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
