package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Song",
        indices = [(Index("artistId")),
            Index("albumId"),
            Index("albumArtistId"),
            Index("genre")])
data class DbSong(
        @PrimaryKey
        val id: Long,
        val song: String,
        val title: String,
        val name: String,
        val artistName: String,
        val artistId: Long,
        val albumName: String,
        val albumId: Long,
        val albumArtistName: String,
        val albumArtistId: Long,
        val filename: String,
        val track: Int,
        val time: Int,
        val year: Int,
        val bitrate: Int,
        val rate: Int,
        val url: String,
        val art: String,
        val preciserating: Int,
        val rating: Int,
        val averagerating: Double,
        val composer: String,
        val comment: String,
        val publisher: String,
        val language: String,
        val genre: String)

data class DbSongDisplay(
        val id: Long,
        val title: String,
        val artistName: String,
        val albumName: String,
        val albumArtistName: String,
        val time: Int,
        val art: String,
        val url: String,
        val genre: String)
