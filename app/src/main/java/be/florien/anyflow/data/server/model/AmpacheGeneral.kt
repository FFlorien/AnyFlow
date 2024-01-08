package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheNameId {
    var id: Long = 0
    var name: String = ""
}

abstract class AmpacheApiResponse<T> {
    val error: AmpacheError? = null
    val total_count: Int = 0
    abstract var list: List<T>
}