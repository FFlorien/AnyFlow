package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Server-side data structures that relate to authentication
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpacheAuthentication: AmpacheAuthenticatedStatus() {
    var auth: String = ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
open class AmpacheAuthenticatedStatus: AmpacheStatus() {
    var session_expire: String = ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
open class AmpacheStatus {
    var server: String = ""
    var version: String = ""
    var songs: Int = 0
    var albums: Int = 0
    var artists: Int = 0
    var playlists: Int = 0
    var update: String = ""
    var add: String = ""
    var clean: String = ""
    var error: AmpacheError = AmpacheError()
}

class AmpacheError {
    var errorAction : String = ""
    var errorType : String = ""
    var errorCode: Int = 0
    var errorMessage: String = "success"
}