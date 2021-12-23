package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheErrorObject {
    var error: AmpacheError = AmpacheError()
}