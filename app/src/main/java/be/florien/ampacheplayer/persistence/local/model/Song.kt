package be.florien.ampacheplayer.persistence.local.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import be.florien.ampacheplayer.persistence.server.model.AmpacheSong


/**
 * Database structure that represents to accounts
 */
@Entity(indices = [Index("artistId"),
    Index("albumId"),
    Index("albumArtistId"),
    Index("genre")])
open class Song {
    @PrimaryKey
    var id: Long = 0
    var song: String = ""
    var title: String = ""
    var name: String = ""
    var artistName: String = ""
    var artistId: Long = -1
    var albumName: String = ""
    var albumId: Long = -1
    var albumArtistName: String = ""
    var albumArtistId: Long = -1
    @Ignore
    var tag: MutableList<Tag> = mutableListOf()
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
