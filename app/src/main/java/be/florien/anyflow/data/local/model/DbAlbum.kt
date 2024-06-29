package be.florien.anyflow.data.local.model

import androidx.room.*

@Entity(
    tableName = "Album",
    foreignKeys = [ForeignKey(
        entity = DbArtist::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("artistId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class DbAlbum(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(index = true)
    val name: String,
    @ColumnInfo(index = true)
    val artistId: Long,
    var prefix: String?,
    var basename: String,
    val year: Int,
    val diskcount: Int
)

data class DbAlbumWithArtist(
    @Embedded
    val album: DbAlbum,
    @Relation(
        parentColumn = "artistId",
        entityColumn = "id"
    )
    val artist: DbArtist
)

data class DbAlbumDisplay(
    val albumId: Long,
    val albumName: String,
    val albumArtistId: Long,
    val year: Int,
    val diskcount: Int,
    val albumArtistName: String,
    val summary: String?
)