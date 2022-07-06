package be.florien.anyflow.data.view

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SongInfo(
    val id: Long,
    val title: String,
    val artistName: String,
    val artistId: Long,
    val albumName: String,
    val albumId: Long,
    val albumArtistName: String,
    val albumArtistId: Long,
    val genreNames: List<String>,
    val track: Int,
    val time: Int,
    val year: Int,
    val local: String?
): Parcelable {
    val timeText: String
        get() = String.format("%d:%02d", time / 60, time % 60)
}

data class FilterGroup(
    val id: Long,
    val name: String
)

@Parcelize
data class Playlist(
    val id: Long,
    val name: String,
    val count: Int
) : Parcelable