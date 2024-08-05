package be.florien.anyflow.management.playlist.model

data class PlaylistSong(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long,
    val time: Int
) {

    val timeText: String
        get() = String.format("%d:%02d", time / 60, time % 60)
}