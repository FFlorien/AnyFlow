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
    val year: Int,
    val diskcount: Int
)

data class DbAlbumDisplay(
    @Embedded
    val album: DbAlbum,
    @Relation(
        parentColumn = "artistId",
        entityColumn = "id"
    )
    val artist: DbArtist
)