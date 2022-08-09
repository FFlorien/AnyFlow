package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Server-side data structures that relates to playlist
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpachePlayListWithSongs {
    var id: Long = 0
    var name: String = ""
    var owner: String = ""
    var items: List<AmpachePlaylistSong> = mutableListOf()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpachePlaylistResponse {
    var playlist: List<AmpachePlayListWithSongs> = mutableListOf()
    var error: AmpacheError? = null
}