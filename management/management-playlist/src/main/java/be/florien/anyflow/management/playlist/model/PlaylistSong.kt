package be.florien.anyflow.management.playlist.model

import be.florien.anyflow.common.utils.TimeOperations

data class PlaylistSong(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long,
    val time: Int
) {

    val timeText: String
        get() = TimeOperations.toMediaDuration(time)
}