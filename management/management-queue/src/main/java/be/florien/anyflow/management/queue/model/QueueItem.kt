package be.florien.anyflow.management.queue.model

import be.florien.anyflow.common.utils.TimeOperations


sealed interface QueueItemDisplay

data object ErrorDisplay: QueueItemDisplay

data class SongDisplay(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long,
    val time: Int
) : QueueItemDisplay {
    val timeText: String
        get() = TimeOperations.toShortDuration(time)
}

data class PodcastEpisodeDisplay(
    val id: Long,
    val title: String,
    val podcast: String,
    val podcastId: Long,
    val description: String,
    val time: Int
) : QueueItemDisplay {

    val timeText: String
        get() = TimeOperations.toShortDuration(time)

}