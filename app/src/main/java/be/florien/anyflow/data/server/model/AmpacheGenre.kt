package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheGenreResponse {
    var genre: List<AmpacheNameId> = mutableListOf()
    var error: AmpacheError? = null
}
