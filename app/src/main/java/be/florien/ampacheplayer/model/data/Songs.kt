package be.florien.ampacheplayer.model.data

import org.simpleframework.xml.*

/**
 * Created by florien on 16/03/17.
 */
@Root(name = "root", strict = false)
class SongList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var songs: List<Song> = mutableListOf()
    @field:Element(name = "error", required = false) var error: Error = Error()

}

@Root(name = "song")
class Song {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "song", required = false) var song: String = ""
    @field:Element(name = "title", required = false) var title: String = ""
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "artist", required = false) var artist: Artist = Artist()
    @field:Element(name = "album", required = false) var album: Album = Album()
    @field:Element(name = "albumartist", required = false) var albumartist: AlbumArtist = AlbumArtist()
    @field:Element(name = "tag", required = false) var tag: Tag = Tag()
    @field:Element(name = "filename", required = false) var filename: String = ""
    @field:Element(name = "track", required = false) var track: Int = 0
    @field:Element(name = "time", required = false) var time: Int = 0
    @field:Element(name = "year", required = false) var year: Int = 0
    @field:Element(name = "bitrate", required = false) var bitrate: Int = 0
    @field:Element(name = "rate", required = false) var rate: Int = 0
    @field:Element(name = "mode", required = false) var mode: String = ""
    @field:Element(name = "mime", required = false) var mime: String = ""
    @field:Element(name = "url", required = false) var url: String = ""
    @field:Element(name = "size", required = false) var size: Int = 0
    @field:Element(name = "mbid", required = false) var mbid: String = ""
    @field:Element(name = "album_mbid", required = false) var album_mbid: String = ""
    @field:Element(name = "artist_mbid", required = false) var artist_mbid: String = ""
    @field:Element(name = "albumartist_mbid", required = false) var albumartist_mbid: String = ""
    @field:Element(name = "art", required = false) var art: String = ""
    @field:Element(name = "preciserating", required = false) var preciserating: Int = 0
    @field:Element(name = "rating", required = false) var rating: Int = 0
    @field:Element(name = "averagerating", required = false) var averagerating: Int = 0
    @field:Element(name = "composer", required = false) var composer: String = ""
    @field:Element(name = "channels", required = false) var channels: String = ""
    @field:Element(name = "comment", required = false) var comment: String = ""
    @field:Element(name = "publisher", required = false) var publisher: String = ""
    @field:Element(name = "language", required = false) var language: String = ""
    @field:Element(name = "replaygain_album_gain", required = false) var replaygain_album_gain: Double = 0.0
    @field:Element(name = "replaygain_album_peak", required = false) var replaygain_album_peak: Double = 0.0
    @field:Element(name = "replaygain_track_gain", required = false) var replaygain_track_gain: Double = 0.0
    @field:Element(name = "replaygain_track_peak", required = false) var replaygain_track_peak: Double = 0.0
    @field:Element(name = "genre", required = false) var genre: String = ""
}

class Artist {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var name: String = ""
}

class Album {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var name: String = ""
}

class AlbumArtist {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var name: String = ""
}

class Tag {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var value: String = ""
    @field:Attribute(name = "count", required = false) var count: Int = 0
}
