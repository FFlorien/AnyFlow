package be.florien.anyflow.tags.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Genre"
)
data class DbGenre(
    @PrimaryKey
    val id: Long,
    val name: String)

@Entity(
    tableName = "SongGenre",
    primaryKeys = ["songId", "genreId"],
    foreignKeys = [ForeignKey(
        entity = DbSong::class,
        parentColumns = ["id"],
        childColumns = ["songId"],
        onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
        entity = DbGenre::class,
        parentColumns = ["id"],
        childColumns = ["genreId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DbSongGenre(
    @ColumnInfo(index = true)
    val songId: Long,
    @ColumnInfo(index = true)
    val genreId: Long
)