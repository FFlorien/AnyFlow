package be.florien.anyflow.management.podcast.model

data class PodcastEpisodeInfo(
    val id: Long,
    val title: String,
    val state: String,
    val time: Int,
    val playCount: Int,
    val played: String,
    val waveForm: String?,
    val description: String,
    val website: String,
    val publicationDate: Long,
    val podcastId: Long,
    val podcast: String,
    val podcastDescription: String,
    val podcastCategory: String,
    val podcastWebsite: String,
    val podcastSyncDate: Long,
    val local: String?
) {
    companion object {
        fun dummyPodcastInfo() = PodcastEpisodeInfo(
            -1,
            "",
            "",
            -1,
            -1,
            "",
            null,
            "",
            "",
            0,
            -1,
            "",
            "",
            "",
            "",
            0,
            null
        )
    }
}

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
    val time: Int,
    val publicationDate: Long
)
