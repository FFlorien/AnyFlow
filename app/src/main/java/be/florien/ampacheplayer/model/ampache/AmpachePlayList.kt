package be.florien.ampacheplayer.model.ampache

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

/**
 * Server-side data structures that relates to playlist
 */

@Root(name = "playlist", strict = false)
class AmpachePlayList {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "owner", required = false) var owner: String = ""
    @field:Element(name = "items", required = false) var items: Int = 0
    @field:ElementList(entry = "tag", inline = true, required = false) var tag: List<AmpacheTagName> = mutableListOf()
}


@Root(name = "root", strict = false)
class AmpachePlayListList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var playlists: List<AmpachePlayList> = mutableListOf()
    @field:Element(name = "error", required = false) var error: AmpacheError = AmpacheError()
}