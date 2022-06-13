package be.florien.anyflow.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
    ForeignKey(
            entity = DbFilterGroup::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("filterGroup"),
            onDelete = ForeignKey.CASCADE)])
data class DbFilter(
        @PrimaryKey(autoGenerate = true)
        val id: Int?,
        val clause: String,
        val joinClause: String?,
        val argument: String,
        val displayText: String,
        val displayImage: String?,
        @ColumnInfo(index = true)
        val filterGroup: Long) {

    companion object {
        const val TITLE_IS = "title ="
        const val TITLE_CONTAIN = "title LIKE"
        const val SEARCH = "title AND genre.name AND artist.name AND album.name LIKE"
        const val GENRE_IS = "songgenre.genreId ="
        const val SONG_ID = "song.id ="
        const val ARTIST_ID = "song.artistId ="
        const val ALBUM_ARTIST_ID = "album.artistId ="
        const val ALBUM_ID = "song.albumId ="
        const val PLAYLIST_ID = "playlistSongs.playlistId ="
        const val PLAYLIST_ID_JOIN = "LEFT JOIN playlistSongs on song.id = playlistSongs.songId"
        const val DOWNLOADED = "song.local IS NOT NULL"
        const val NOT_DOWNLOADED = "song.local IS NULL"
    }
}