package be.florien.ampacheplayer.model.server

import org.simpleframework.xml.*

/**
 * Data structures that relates to tags
 */
@Root(name = "tag", strict = false)
class TagServer {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Element(name = "name", required = false) var name: String = ""
    @field:Element(name = "albums", required = false) var albums: Int = 0
    @field:Element(name = "artists", required = false) var artists: Int = 0
    @field:Element(name = "songs", required = false) var songs: Int = 0
    @field:Element(name = "video", required = false) var video: Int = 0
    @field:Element(name = "playlist", required = false) var playlist: Int = 0
    @field:Element(name = "stream", required = false) var stream: Int = 0
}

@Root(name = "root", strict = false)
class TagList {
    @field:Element(name = "total_count", required = false) var total_count: Int = 0
    @field:ElementList(inline = true, required = false) var tags: List<TagServer> = mutableListOf()
    @field:Element(name = "error", required = false) var error: Error = Error()
}

class TagName {
    @field:Attribute(name = "id", required = false) var id: Long = 0
    @field:Text() var value: String = ""
    @field:Attribute(name = "count", required = false) var count: Int = 0
}