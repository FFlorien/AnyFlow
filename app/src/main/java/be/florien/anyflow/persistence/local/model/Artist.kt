package be.florien.anyflow.persistence.local.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import be.florien.anyflow.persistence.server.model.AmpacheArtist

/**
 * Database structure that represents to artists
 */
@Entity
data class Artist (
    @field:PrimaryKey
    val id: Long,
    val name: String,
    val preciserating: Int,
    val rating: Double = 0.0) {

    constructor(fromServer: AmpacheArtist) : this(
        fromServer.id,
        fromServer.name,
        fromServer.preciserating,
        fromServer.rating)
}

data class ArtistDisplay(
        val id: Long,
        val name: String)
