package be.florien.ampacheplayer.model.server

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

/**
 * Server-side data structures that relates to playlist
 */

@Root(name = "playlist", strict = false)
class PlaylistServer {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "owner", required = false) var owner: String = ""
    @field:Element(name = "items", required = false) var items: Int = 0
    @field:ElementList(entry = "tag", inline = true, required = false) var tag: List<TagName> = mutableListOf()
}


@Root(name = "root", strict = false)
class PlaylistList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var playlists: List<PlaylistServer> = mutableListOf()
    @field:Element(name = "error", required = false) var error: Error = Error()
}