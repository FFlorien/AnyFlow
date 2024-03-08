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
    @JsonProperty("type")
    var playlistList: AmpachePlaylistsWithSongs = AmpachePlaylistsWithSongs()
}

class AmpachePlaylistsWithSongs {
    val playlists: Map<String, AmpachePlaylistWithSongs> = mutableMapOf()

    @JsonAnySetter
    fun addToPlaylistMap(playlistId: String, songsId: AmpachePlaylistWithSongs) {
        (playlists as MutableMap)[playlistId] = songsId
    }
}

class AmpachePlaylistWithSongs {
    @JsonProperty("song")
    var songsId: List<Long> = mutableListOf()
}