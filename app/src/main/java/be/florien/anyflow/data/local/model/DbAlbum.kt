package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import be.florien.anyflow.data.server.model.AmpacheAlbum

@Entity(tableName = "Album", indices = [Index("artistId"), Index("name")])
data class DbAlbum(
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
        val rating: Double)

data class DbAlbumDisplay(
        val id: Long,
        val name: String,
        val artistName: String,
        val art: String)