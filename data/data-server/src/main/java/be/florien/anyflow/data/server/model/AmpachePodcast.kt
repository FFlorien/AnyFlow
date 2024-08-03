package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Server-side data structures that relates to podcasts
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpachePodcast {
    var id: String = ""
    var name: String = ""
    var description: String = ""
    var language: String = ""
    var copyright: String = ""
    var feed_url: String = ""
    var website: String = ""
    var build_date: String = ""
    var sync_date: String = ""
    var podcast_episode: List<AmpachePodcastEpisode> = mutableListOf()
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpachePodcastEpisode {
    var id: String = ""
    var title: String = ""
    var podcast: AmpacheNameId = AmpacheNameId()
    var description: String = ""
    var category: String = ""
    var author_full: String = ""
    var website: String = ""
    var pubdate: String = ""
    var state: String = ""
    var time: Int = 0
    var size: Int = 0
    var stream_bitrate: Int = 0
    var public_url: String = ""
    var playcount: Int = 0
    var played: String = ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpachePodcastsResponse: AmpacheApiListResponse<AmpachePodcast>() {
    @JsonProperty(value = "podcast")
    override var list: List<AmpachePodcast> = mutableListOf()
}
