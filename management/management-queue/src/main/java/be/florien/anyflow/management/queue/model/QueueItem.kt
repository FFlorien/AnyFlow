package be.florien.anyflow.management.queue.model




sealed class QueueItemDisplay { //todo this is a bit shit, just sealed class would be better
    abstract val id: Long
    abstract val title: String
    abstract val artist: String
    abstract val album: String
    abstract val albumId: Long
    abstract val time: Int

    val timeText: String
        get() = String.format("%d:%02d", time / 60, time % 60)
}

data class SongDisplay(
    override val id: Long,
    override val title: String,
    val artistName: String,
    val albumName: String,
    override val albumId: Long,
    override val time: Int
): QueueItemDisplay() {
    override val artist: String
        get() = artistName
    override val album: String
        get() = albumName
}

data class PodcastEpisodeDisplay( //todo shitty shit
    override val id: Long,
    override val title: String,
    override val artist: String,
    override val album: String,
    override val albumId: Long,
    override val time: Int
): QueueItemDisplay()