package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Server-side data structures that relates to playlist
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpachePlayList {
    var id: Long = 0
    var name: String = ""
    var owner: String = ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpachePlaylistResponse: AmpacheApiListResponse<AmpachePlayList>() {
    @JsonProperty("playlist")
    override var list: List<AmpachePlayList> = mutableListOf()
}

class AmpachePlaylistsWithSongsResponse: AmpacheApiResponse() {
    @JsonProperty("playlist")
    var playlistList: AmpachePlaylistsWithSongs = AmpachePlaylistsWithSongs()
}

class AmpachePlaylistsWithSongs {
    val playlists: Map<String, List<AmpachePlaylistObject>> = mutableMapOf()

    @JsonAnySetter
    fun addToPlaylistMap(playlistId: String, songsId: List<AmpachePlaylistObject>) {
        (playlists as MutableMap)[playlistId] = songsId
    }
}

class AmpachePlaylistObject {
    @JsonProperty("id")
    var id: Long = 0
    @JsonProperty("type")
    var type: String = ""
}