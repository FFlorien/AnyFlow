package be.florien.anyflow.persistence.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import be.florien.anyflow.persistence.server.model.AmpacheAlbum

@Entity(indices = [Index("artistId"), Index("name")])
data class Album(
        @PrimaryKey
        val id: Long,
        val name: String,
        val artistName: String,
        val artistId: Long,
        val year: Int,
        val tracks: Int,
        val disk: Int,
        val art: String,
        val preciserating: Int,
        val rating: Double) {

    constructor(fromServer: AmpacheAlbum) : this(
            fromServer.id,
            fromServer.name,
            fromServer.artist.name,
            fromServer.artist.id,
            fromServer.year,
            fromServer.tracks,
            fromServer.disk,
            fromServer.art,
            fromServer.preciserating,
            fromServer.rating)
}

data class AlbumDisplay(
        val id: Long,
        val name: String,
        val artistName: String,
        val art: String)