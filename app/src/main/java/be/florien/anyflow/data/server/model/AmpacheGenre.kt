package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheGenreResponse: AmpacheApiResponse<AmpacheNameId>() {
    @JsonProperty("genre")
    override var list: List<AmpacheNameId> = mutableListOf()
}
