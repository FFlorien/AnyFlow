package be.florien.ampacheplayer.model.data

import org.simpleframework.xml.*

/**
 * Data structures that relates to album
 */
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

@Root(name = "root", strict = false)
class AlbumList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var albums: List<Album> = mutableListOf()
    @field:Element(name = "error", required = false) var error: Error = Error()

}

class AlbumName {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var name: String = ""
}