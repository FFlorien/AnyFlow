package be.florien.anyflow.data.local.model

import androidx.room.*

@Entity(
    tableName = "Song"
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
    val size: Int,
    val local: String?,
    val waveForm: String
)

data class DbSongInfo(
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
    var genres: List<DbGenre>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(DbPlaylistSongs::class,
            parentColumn = "songId",
            entityColumn = "playlistId")
    )
    var playlists: List<DbPlaylist>
)

data class DbSongDisplay(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long,
    val time: Int
)

data class DbSongToPlay(
    val id: Long,
    val local: String?
)

data class DbSongId(
    val id: Long
)

data class DbSongWaveForm(
    val waveForm: String
) {
    val downSamplesArray: DoubleArray
    get() {
        if (waveForm.isBlank()) return DoubleArray(0)
        return waveForm.split('|').map { it.replace(',', '.').toDouble() }.toDoubleArray()
    }
}
