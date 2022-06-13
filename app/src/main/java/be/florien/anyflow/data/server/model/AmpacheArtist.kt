package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Server-side data structures that relates to artists
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpacheArtist {
    var id: Long = 0
    var name: String = ""
    var summary: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheArtistResponse {
    var artist: List<AmpacheArtist> = mutableListOf()
    var error: AmpacheError? = null
}
