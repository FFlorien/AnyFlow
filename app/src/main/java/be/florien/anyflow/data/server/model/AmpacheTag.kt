package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Server-side data structures that relates to tags
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpacheTag {
    var id: Long = 0
    var name: String = ""
    var albums: Int = 0
    var artists: Int = 0
    var songs: Int = 0
    var video: Int = 0
    var playlist: Int = 0
    var stream: Int = 0
}

class AmpacheTagName {
    var id: Long = 0
    var name: String = ""
}