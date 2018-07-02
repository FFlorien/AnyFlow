package be.florien.ampacheplayer.persistence.local.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Relation
import be.florien.ampacheplayer.persistence.server.model.AmpacheAlbum

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