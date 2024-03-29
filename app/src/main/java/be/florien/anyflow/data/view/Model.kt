package be.florien.anyflow.data.view

import android.os.Parcelable
import be.florien.anyflow.extension.ImageConfig
import kotlinx.parcelize.Parcelize

@Parcelize
data class SongInfo constructor(
    val id: Long,
    val title: String,
    val artistName: String,
    val artistId: Long,
    val albumName: String,
    val albumId: Long,
    val disk: Int,
    val albumArtistName: String,
    val albumArtistId: Long,
    val genreNames: List<String>,
    val genreIds: List<Long>,
    val playlistNames: List<String>,
    val playlistIds: List<Long>,
    val track: Int,
    val time: Int,
    val year: Int,
    val size: Int,
    val local: String?
): Parcelable {
    val timeText: String
        get() = String.format("%d:%02d", time / 60, time % 60)

    companion object {
        fun dummySongInfo(id: Long = 0L) = SongInfo(id, "", "", 0L, "", 0L, 1, "", 0, listOf(""),  listOf(0L), listOf(""),  listOf(0L), 0, 0, 0, 0, "")
    }
}

data class SongDisplay(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long,
    val time: Int) {

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
    val count: Int,
    val coverConfig: ImageConfig
) : Parcelable

@Parcelize
data class PlaylistWithPresence(
    val id: Long,
    val name: String,
    val count: Int,
    val presence: Int,
    val coverConfig: ImageConfig
) : Parcelable