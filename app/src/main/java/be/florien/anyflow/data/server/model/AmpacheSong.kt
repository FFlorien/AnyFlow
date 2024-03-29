package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

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
    var size: Int = 0
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheSongId {
    var id: Long = 0
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpachePlaylistSong {
    var id: Long = 0
    var playlisttrack: Int = 0
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheSongResponse: AmpacheApiResponse<AmpacheSong>() {
    @JsonProperty(value = "song")
    override var list: List<AmpacheSong> = mutableListOf()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheDeletedSongIdResponse: AmpacheApiResponse<AmpacheSongId>() {
    @JsonProperty("deleted_song")
    override var list: List<AmpacheSongId> = mutableListOf()
}
