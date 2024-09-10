package be.florien.anyflow.tags.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Filter",
    foreignKeys = [
        ForeignKey(
            entity = DbFilterGroup::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("filterGroup"),
            onDelete = ForeignKey.CASCADE
        )]
)
data class DbFilter(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val type: Int,
    val argument: String,
    val displayText: String,
    @ColumnInfo(index = true)
    val filterGroup: Long,
    val parentFilter: Long? = null
) {

    companion object {
        // Songs
        const val TYPE_GENRE = 0
        const val TYPE_SONG = 1
        const val TYPE_ARTIST = 2
        const val TYPE_ALBUM_ARTIST = 3
        const val TYPE_ALBUM = 4
        const val TYPE_DISK = 5
        const val TYPE_PLAYLIST = 6
        // Podcasts
        const val TYPE_PODCAST_EPISODE = 7
        // Common
        const val TYPE_DOWNLOADED = 8
    }
}

data class DbTagsFilterCount(
    val duration: Int,
    val genres: Int,
    val albumArtists: Int,
    val albums: Int,
    val artists: Int,
    val songs: Int,
    val playlists: Int,
    val downloaded: Int
)

data class DbPodcastFilterCount(
    val podcastEpisodes: Int
)