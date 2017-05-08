package be.florien.ampacheplayer.model.ampache

import org.simpleframework.xml.*

/**
 * Server-side data structures that relates to artists
 */
@Root(name = "artist", strict = false)
class AmpacheArtist {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "albums", required = false) var albums: String = ""
    @field:Element(name = "songs", required = false) var songs: String = ""
    @field:ElementList(entry = "tag", inline = true, required = false) var tag: List<AmpacheTagName> = mutableListOf()
    @field:Element(name = "preciserating", required = false) var preciserating: Int = 0
    @field:Element(name = "rating", required = false) var rating: Double = 0.0

}

@Root(name = "root", strict = false)
class AmpacheArtistList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var artists: List<AmpacheArtist> = mutableListOf()
    @field:Element(name = "error", required = false) var error: AmpacheError = AmpacheError()
}

class AmpacheAlbumArtist {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var name: String = ""
}

class AmpacheArtistName {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var name: String = ""
}
