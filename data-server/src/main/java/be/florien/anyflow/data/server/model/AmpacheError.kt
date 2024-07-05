package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheErrorObject {
    var error: AmpacheError = AmpacheError()
}

class AmpacheError {
    var errorAction : String = ""
    var errorType : String = ""
    var errorCode: Int = 0
    var errorMessage: String = "success"
}