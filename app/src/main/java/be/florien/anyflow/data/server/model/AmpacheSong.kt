package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Server-side data structures that relates to accounts
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpacheSong {
    var id: Long = 0
    var title: String = ""
    var artist: AmpacheNameId = AmpacheNameId()
    var album: AmpacheNameId = AmpacheNameId()
    var genre: List<AmpacheNameId> = mutableListOf()
    var track: Int = 0
    var disk: Int = 0
    var time: Int = 0
    var year: Int = 0
    var composer: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheSongId {
    var id: Long = 0
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheSongResponse {
    var song: List<AmpacheSong> = mutableListOf()
    var error: AmpacheError? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheSongIdResponse {
    var song: List<AmpacheSongId> = mutableListOf()
    var error: AmpacheError? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheDeletedSongIdResponse {
    var deleted_song: List<AmpacheSongId> = mutableListOf()
    var error: AmpacheError? = null
}
