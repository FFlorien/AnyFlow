package be.florien.ampacheplayer.business.realm

import be.florien.ampacheplayer.business.ampache.AmpacheSong
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

/**
 * Database structure that represents to songs
 */
@RealmClass
open class RealmSong(
        @field:PrimaryKey
    open var id: Long = 0,
        open var song: String = "",
        open var title: String = "",
        open var name: String = "",
        open var artistName: String = "",
        open var artistId: Long = -1,
        open var albumName: String = "",
        open var albumId: Long = -1,
        open var albumArtistName: String = "",
        open var albumArtistId: Long = -1,
        open var tag: RealmList<RealmTag> = RealmList(),
        open var filename: String = "",
        open var track: Int = 0,
        open var time: Int = 0,
        open var year: Int = 0,
        open var bitrate: Int = 0,
        open var rate: Int = 0,
        open var url: String = "",
        open var art: String = "",
        open var preciserating: Int = 0,
        open var rating: Int = 0,
        open var averagerating: Int = 0,
        open var composer: String = "",
        open var comment: String = "",
        open var publisher: String = "",
        open var language: String = "",
        open var genre: String = ""
): RealmObject() {

    constructor(fromServer: AmpacheSong) : this() {
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
        tag.addAll(fromServer.tag.map(::RealmTag))
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
