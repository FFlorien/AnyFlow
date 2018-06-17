package be.florien.ampacheplayer.persistence.local.model

import android.arch.persistence.room.Entity

sealed class Filter {
    var filterFunction: String = ""

    open class StringFilter(filterFunction: String, var argument: String) : Filter(){
        init {
            this.filterFunction = filterFunction
        }
    }

    open class LongFilter(filterFunction: String, var argument: Long) : Filter(){
        init {
            this.filterFunction = filterFunction
        }
    }

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