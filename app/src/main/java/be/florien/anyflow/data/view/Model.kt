package be.florien.anyflow.data.view

import android.os.Parcelable
import be.florien.anyflow.extension.ImageConfig
import kotlinx.parcelize.Parcelize
import java.util.Calendar

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
) {

    val timeText: String
        get() = String.format("%d:%02d", time / 60, time % 60)
}

sealed class FilterGroup(
    open val id: Long
) {
    data class CurrentFilterGroup(override val id: Long) : FilterGroup(id)
    data class HistoryFilterGroup(override val id: Long, val dateAdded: Calendar) : FilterGroup(id)
    data class SavedFilterGroup(override val id: Long, val dateAdded: Calendar, val name: String) :
        FilterGroup(id)
}

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

@Parcelize
data class Podcast(
    val id: String,
    val name: String,
    val description: String,
    val syncDate: String,
    val art: String?,
    val episodes: List<PodcastEpisode>? = null
) : Parcelable

@Parcelize
data class PodcastEpisode(
    val id: String,
    val title: String,
    val description: String,
    val authorFull: String,
    val publicationDate: String,
    val state: String,
    val time: Int,
    val url: String,
    val art: String?,
    val playCount: Int,
    val played: String
) : Parcelable