package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
open class AmpacheErrorResponse {
    var error: AmpacheError? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheSuccessResponse: AmpacheErrorResponse() {
    var success: String? = null
}

abstract class AmpacheApiListResponse<T>: AmpacheErrorResponse() {
    val total_count: Int = 0
    abstract var list: List<T>
}

class AmpacheError {
    var errorAction : String = ""
    var errorType : String = ""
    var errorCode: Int = 0
    var errorMessage: String = "success"
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheNameId {
    var id: Long = 0
    var name: String = ""
}