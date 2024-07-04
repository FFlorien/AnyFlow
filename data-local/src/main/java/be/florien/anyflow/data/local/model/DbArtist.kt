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
        var prefix: String?,
        var basename: String,
        val summary: String?)
