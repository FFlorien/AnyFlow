package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheNameId {
    var id: Long = 0
    var name: String = ""
}

abstract class AmpacheApiResponse {
    val error: AmpacheError? = null
}

abstract class AmpacheApiListResponse<T>: AmpacheApiResponse() {
    val total_count: Int = 0
    abstract var list: List<T>
}