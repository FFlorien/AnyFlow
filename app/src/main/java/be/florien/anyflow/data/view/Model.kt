package be.florien.anyflow.data.view

import android.os.Parcelable
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
        val id: Long,
        val title: String,
        val artistName: String,
        val albumName: String,
        val albumArtistName: String,
        val time: Int,
        val art: String,
        val url: String,
        val genre: String): Parcelable {
    val timeText: String
        get() = String.format("%d:%02d", time / 60, time % 60)
}
data class SongInfo(
        val id: Long,
        val title: String,
        val artistName: String,
        val artistId: Long,
        val albumName: String,
        val albumId: Long,
        val albumArtistName: String,
        val albumArtistId: Long,
        val track: Int,
        val time: Int,
        val year: Int,
        val url: String,
        val art: String,
        val genre: String){
    val timeText: String
        get() = String.format("%d:%02d", time / 60, time % 60)
}

data class FilterGroup(
        val id: Long,
        val name: String)