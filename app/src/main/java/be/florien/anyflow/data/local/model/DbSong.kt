package be.florien.anyflow.data.local.model

import androidx.room.*

@Entity(
    tableName = "Song",
)
data class DbSong(
    @PrimaryKey
    val id: Long,
    val title: String,
    @ColumnInfo(index = true)
    val artistId: Long,
    @ColumnInfo(index = true)
    val albumId: Long,
    val track: Int,
    val disk: Int,
    val time: Int,
    val year: Int,
    val composer: String,
    val local: String?
)

data class DbSongDisplay(
    @Embedded
    var song: DbSong,
    @Relation(
        parentColumn = "artistId",
        entityColumn = "id"
    )
    var artist: DbArtist,
    @Relation(
        entity = DbAlbum::class,
        parentColumn = "albumId",
        entityColumn = "id"
    )
    var album: DbAlbumDisplay,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(DbSongGenre::class,
        parentColumn = "songId",
        entityColumn = "genreId")
    )
    var genres: List<DbGenre>
)

data class DbSongToPlay(
    val id: Long,
    val local: String?
)

data class DbSongId(
    val id: Long
)
