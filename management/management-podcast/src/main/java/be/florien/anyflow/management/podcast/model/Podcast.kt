package be.florien.anyflow.management.podcast.model



data class PodcastEpisodeDisplay(
    val id: Long,
    val title: String,
    val author: String,
    val album: String,
    val albumId: Long,
    val time: Int
)
