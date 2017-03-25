package be.florien.ampacheplayer.model.data

import io.realm.RealmList
import io.realm.RealmObject
import org.simpleframework.xml.*

/**
 * Created by florien on 16/03/17.
 */
@Root(name = "root", strict = false)
class SongList : RealmObject() {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var songs: List<Song> = mutableListOf()
    @field:Element(name = "error", required = false) var error: Error = Error()

}

@Root(name = "root", strict = false)
class ArtistList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var artists: RealmList<Artist> = RealmList()
    @field:Element(name = "error", required = false) var error: Error = Error()

}

@Root(name = "root", strict = false)
class AlbumList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var albums: List<Album> = mutableListOf()
    @field:Element(name = "error", required = false) var error: Error = Error()

}

@Root(name = "root", strict = false)
class TagList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var tags: List<Tag> = mutableListOf()
    @field:Element(name = "error", required = false) var error: Error = Error()

}

@Root(name = "root", strict = false)
class PlaylistList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var playlists: List<Playlist> = mutableListOf()
    @field:Element(name = "error", required = false) var error: Error = Error()

}

@Root(name = "song", strict = false)
class Song {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "song", required = false) var song: String = ""
    @field:Element(name = "title", required = false) var title: String = ""
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "artist", required = false) var artist: ArtistName = ArtistName()
    @field:Element(name = "album", required = false) var album: AlbumName = AlbumName()
    @field:Element(name = "albumartist", required = false) var albumartist: AlbumArtist = AlbumArtist()
    @field:ElementList(entry = "tag", inline = true, required = false) var tag: List<TagName> = mutableListOf()
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

@Root(name = "artist", strict = false)
class Artist : RealmObject(){
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "albums", required = false) var albums: String = ""
    @field:Element(name = "songs", required = false) var songs: String = ""
    @field:ElementList(entry = "tag", inline = true, required = false) var tag: List<TagName> = mutableListOf()
    @field:Element(name = "preciserating", required = false) var preciserating: Int = 0
    @field:Element(name = "rating", required = false) var rating: Double = 0.0

}

@Root(name = "album", strict = false)
class Album {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "artist ", required = false) var artist: ArtistName = ArtistName()
    @field:Element(name = "year", required = false) var year: Int = 0
    @field:Element(name = "tracks", required = false) var tracks: Int = 0
    @field:Element(name = "disk", required = false) var disk: Int = 0
    @field:ElementList(entry = "tag", inline = true, required = false) var tag: List<TagName> = mutableListOf()
    @field:Element(name = "art", required = false) var art: String = ""
    @field:Element(name = "preciserating", required = false) var preciserating: Int = 0
    @field:Element(name = "rating", required = false) var rating: Double = 0.0
}

@Root(name = "tag", strict = false)
class Tag {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "albums", required = false) var albums: Int = 0
    @field:Element(name = "artists", required = false) var artists: Int = 0
    @field:Element(name = "songs", required = false) var songs: Int = 0
    @field:Element(name = "video", required = false) var video: Int = 0
    @field:Element(name = "playlist", required = false) var playlist: Int = 0
    @field:Element(name = "stream", required = false) var stream: Int = 0
}

@Root(name = "playlist", strict = false)
class Playlist {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "owner", required = false) var owner: String = ""
    @field:Element(name = "items", required = false) var items: Int = 0
    @field:ElementList(entry = "tag", inline = true, required = false) var tag: List<TagName> = mutableListOf()
}

class ArtistName {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var name: String = ""
}

class AlbumName {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var name: String = ""
}

class AlbumArtist {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var name: String = ""
}

class TagName {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var value: String = ""
    @field:Attribute(name = "count", required = false) var count: Int = 0
}
