package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Server-side data structures that relates to album
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpacheAlbum {
    var id: Long = 0
    var name: String = ""
    var prefix: String? = null
    var basename: String = ""
    var artist: AmpacheNameId = AmpacheNameId()
    var year: Int = 0
    var diskcount: Int = 0
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheAlbumResponse {
    var album: List<AmpacheAlbum> = mutableListOf()
    var error: AmpacheError? = null
}