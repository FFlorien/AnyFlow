package be.florien.ampacheplayer.business.ampache

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import org.simpleframework.xml.Text

/**
 * Server-side data structures that relate to authentication
 */
@Root(name = "root")
class AmpacheAuthentication {
    @field:Element(name = "session_expire", required = false) var sessionExpire: String = ""
    @field:Element(name = "update", required = false) var update: String = ""
    @field:Element(name = "tags", required = false) var tags: String = ""
    @field:Element(name = "artists", required = false) var artists: String = ""
    @field:Element(name = "clean", required = false) var clean: String = ""
    @field:Element(name = "songs", required = false) var songs: String = ""
    @field:Element(name = "albums", required = false) var albums: String = ""
    @field:Element(name = "playlists", required = false) var playlists: String = ""
    @field:Element(name = "catalogs", required = false) var catalogs: String = ""
    @field:Element(name = "videos", required = false) var videos: String = ""
    @field:Element(name = "add", required = false) var add: String = ""
    @field:Element(name = "auth", required = false) var auth: String = ""
    @field:Element(name = "api", required = false) var api: String = ""
    @field:Element(name = "version", required = false) var version: String = ""
    @field:Element(name = "error", required = false) var error: AmpacheError = AmpacheError()
}

@Root(name = "root")
class AmpachePing {
    @field:Element(name = "session_expire", required = false) var sessionExpire: String = ""
    @field:Element(name = "server", required = false) var server: String = ""
    @field:Element(name = "version", required = false) var version: String = ""
    @field:Element(name = "compatible", required = false) var compatible: String = ""
    @field:Element(name = "error", required = false) var error: AmpacheError = AmpacheError()
}

@Root(name = "error")
class AmpacheError {
    @field:Attribute(name = "code")
    var code: Int = 0
    @field:Text
    var errorText: String= "success"
}