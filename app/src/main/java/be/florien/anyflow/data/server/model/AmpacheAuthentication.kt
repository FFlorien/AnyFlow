package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Server-side data structures that relate to authentication
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheAuthentication {
    var session_expire: String = ""
    var auth: String = ""
    var error: AmpacheError = AmpacheError()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpachePing {
    var session_expire: String = ""
    var server: String = ""
    var version: String = ""
    var compatible: String = ""
    var error: AmpacheError = AmpacheError()
}

class AmpacheError {
    var code: Int = 0
    var error_text: String = "success"
}