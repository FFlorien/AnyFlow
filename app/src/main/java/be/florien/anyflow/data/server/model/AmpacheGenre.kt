package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheGenreResponse: AmpacheApiListResponse<AmpacheNameId>() {
    @JsonProperty("genre")
    override var list: List<AmpacheNameId> = mutableListOf()
}
