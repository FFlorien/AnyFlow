package be.florien.ampacheplayer.business.ampache

import org.simpleframework.xml.*

/**
 * Server-side data structures that relates to album
 */
@Root(name = "album", strict = false)
class AmpacheAlbum {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "artistName ", required = false) var artist: AmpacheArtistName = AmpacheArtistName()
    @field:Element(name = "year", required = false) var year: Int = 0
    @field:Element(name = "tracks", required = false) var tracks: Int = 0
    @field:Element(name = "disk", required = false) var disk: Int = 0
    @field:ElementList(entry = "tag", inline = true, required = false) var tag: List<AmpacheTagName> = mutableListOf()
    @field:Element(name = "art", required = false) var art: String = ""
    @field:Element(name = "preciserating", required = false) var preciserating: Int = 0
    @field:Element(name = "rating", required = false) var rating: Double = 0.0
}

@Root(name = "root", strict = false)
class AmpacheAlbumList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var albums: List<AmpacheAlbum> = mutableListOf()
    @field:Element(name = "error", required = false) var error: AmpacheError = AmpacheError()

}

class AmpacheAlbumName {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var name: String = ""
}