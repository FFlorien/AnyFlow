package be.florien.anyflow.persistence.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import be.florien.anyflow.persistence.server.model.AmpacheArtist

/**
 * Database structure that represents to artists
 */
@Entity
data class Artist(
        @field:PrimaryKey
        val id: Long,
        val name: String,
        val preciserating: Int,
        val rating: Double = 0.0,
        val art: String = "") {

    constructor(fromServer: AmpacheArtist) : this(
            fromServer.id,
            fromServer.name,
            fromServer.preciserating,
            fromServer.rating,
            fromServer.art)
}

data class ArtistDisplay(
        val id: Long,
        val name: String,
        val art: String)
