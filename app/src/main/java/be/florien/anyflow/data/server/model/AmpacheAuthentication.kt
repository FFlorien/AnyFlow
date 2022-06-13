package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Server-side data structures that relate to authentication
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpacheAuthentication {
    var session_expire: String = ""
    var auth: String = ""
    var songs: Int = 0
    var albums: Int = 0
    var artists: Int = 0
    var playlists: Int = 0
    var error: AmpacheError = AmpacheError()
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpachePing {
    var session_expire: String = ""
    var server: String = ""
    var version: String = ""
    var compatible: String = ""
    var songs: Int = 0
    var albums: Int = 0
    var artists: Int = 0
    var playlists: Int = 0
    var error: AmpacheError = AmpacheError()
}

class AmpacheError {
    var errorAction : String = ""
    var errorType : String = ""
    var errorCode: Int = 0
    var errorMessage: String = "success"
}