package be.florien.anyflow.tags.model

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
) : Parcelable {
    val timeText: String
        get() = String.format("%d:%02d", time / 60, time % 60)

    companion object {
        fun dummySongInfo(id: Long = 0L) = SongInfo(
            id,
            "",
            "",
            0L,
            "",
            0L,
            1,
            "",
            0,
            listOf(""),
            listOf(0L),
            listOf(""),
            listOf(0L),
            0,
            0,
            0,
            0,
            ""
        )
    }
}


data class SongDisplay(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long,
    val time: Int
)