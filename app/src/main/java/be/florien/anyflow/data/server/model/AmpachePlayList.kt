package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Server-side data structures that relates to playlist
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpachePlayList {
    var id: Long = 0
    var name: String = ""
    var owner: String = ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpachePlaylistResponse {
    var playlist: List<AmpachePlayList> = mutableListOf()
    var error: AmpacheError? = null
}