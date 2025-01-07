package be.florien.anyflow.management.podcast.model

data class PodcastDisplay(
    val id: Long,
    val name: String,
    val lastUpdate: Long
)

data class PodcastEpisodeDisplay(
    val id: Long,
    val title: String,
    val podcast: String,
    val podcastId: Long,
    val time: Int
)
