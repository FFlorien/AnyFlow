package be.florien.anyflow.tags.local.model

import androidx.room.*

@Entity(
    tableName = "Song"
)
data class DbSong(
    @PrimaryKey
    val id: Long,
    val title: String,
    @ColumnInfo(index = true)
    val titleForSort: String,
    @ColumnInfo(index = true)
    val artistId: Long,
    @ColumnInfo(index = true)
    val albumId: Long,
    @ColumnInfo(index = true)
    val track: Int,
    @ColumnInfo(index = true)
    val disk: Int,
    val time: Int,
    @ColumnInfo(index = true)
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
    var album: DbAlbumWithArtist,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            DbSongGenre::class,
            parentColumn = "songId",
            entityColumn = "genreId"
        )
    )
    var genres: List<DbGenre>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            DbPlaylistSongs::class,
            parentColumn = "songId",
            entityColumn = "playlistId"
        )
    )
    var playlists: List<DbPlaylist>
)

data class DbSongDisplay(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long,
    val time: Int,
    val titleForSort: String
)

data class DbQueueItemDisplay(
    // Common
    val mediaType: Int,
    // Song
    val songId: Long?,
    val songTitle: String?,
    val songArtistName: String?,
    val songAlbumName: String?,
    val songAlbumId: Long?,
    val songTime: Int?,
    // Podcast
    val podcastEpisodeId: Long?,
    val podcastTitle: String?,
    val podcastName: String?,
    val podcastTime: Int?,
    val podcastId: Long?,
    val podcastDescription: String?
)

data class DbSongId(
    val id: Long
)

data class DbMediaWaveForm(
    val waveForm: String?
) {
    val downSamplesArray: DoubleArray
        get() {
            if (waveForm.isNullOrBlank()) return DoubleArray(0)
            return waveForm.split('|').map { it.replace(',', '.').toDouble() }.toDoubleArray()
        }
}
