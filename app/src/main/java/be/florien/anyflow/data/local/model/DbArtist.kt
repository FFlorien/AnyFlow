package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database structure that represents to artists
 */
@Entity(tableName = "Artist")
data class DbArtist(
        @field:PrimaryKey
        val id: Long,
        val name: String,
        val preciserating: Int,
        val rating: Double = 0.0,
        val art: String = "")

data class DbArtistDisplay(
        val id: Long,
        val name: String,
        val art: String?)
