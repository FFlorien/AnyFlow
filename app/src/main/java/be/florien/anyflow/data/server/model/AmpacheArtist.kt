package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Server-side data structures that relates to artists
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpacheArtist {
    var id: Long = 0
    var name: String = ""
    var prefix: String? = null
    var basename: String = ""
    var summary: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheArtistResponse: AmpacheApiResponse<AmpacheArtist>() {
    @JsonProperty("artist")
    override var list: List<AmpacheArtist> = mutableListOf()
}
