package be.florien.ampacheplayer.persistence.model

import android.arch.persistence.room.Entity

@Entity
sealed class Filter(
        private var filterFunction: String) {

    @Entity
    open class StringFilter(filterFunction: String, var argument: String): Filter(filterFunction)

    @Entity
    open class LongFilter(filterFunction: String, var argument: Long): Filter(filterFunction)

    /**
     * String filters
     */
    class TitleIs(argument: String) : StringFilter("title = ", argument)

    class TitleContain(argument: String) : StringFilter("title LIKE ", argument)

    class GenreIs(argument: String) : StringFilter("song.genre = ", argument)

    /**
     * Long filters
     */

    class SongIs(argument: Long) : LongFilter("song.id = ", argument)

    class ArtistIs(argument: Long) : LongFilter("song.artistId = ", argument)

    class AlbumArtistIs(argument: Long) : LongFilter("song.albumArtistId = ", argument)

    class AlbumIs(argument: Long) : LongFilter("song.albumId = ", argument)
}