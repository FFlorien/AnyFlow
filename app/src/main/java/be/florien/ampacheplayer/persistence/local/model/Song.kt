package be.florien.ampacheplayer.persistence.local.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import be.florien.ampacheplayer.persistence.server.model.AmpacheSong

@Entity(indices = [(Index("artistId")),
    Index("albumId"),
    Index("albumArtistId"),
    Index("genre")])
data class Song(
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
        val genre: String) {

    constructor(fromServer: AmpacheSong) : this(
            fromServer.id,
            fromServer.song,
            fromServer.title,
            fromServer.name,
            fromServer.artist.name,
            fromServer.artist.id,
            fromServer.album.name,
            fromServer.album.id,
            fromServer.albumartist.name,
            fromServer.albumartist.id,
            fromServer.filename,
            fromServer.track,
            fromServer.time,
            fromServer.year,
            fromServer.bitrate,
            fromServer.rate,
            fromServer.url,
            fromServer.art,
            fromServer.preciserating,
            fromServer.rating,
            fromServer.averagerating,
            fromServer.composer,
            fromServer.comment,
            fromServer.publisher,
            fromServer.language,
            fromServer.genre)
}

data class SongDisplay(
        val id: Long,
        val title: String,
        val artistName: String,
        val albumName: String,
        val albumArtistName: String,
        val time: Int,
        val art: String) {

    constructor(song: Song) : this(
            song.id,
            song.title,
            song.artistName,
            song.albumName,
            song.albumArtistName,
            song.time,
            song.art)
}
