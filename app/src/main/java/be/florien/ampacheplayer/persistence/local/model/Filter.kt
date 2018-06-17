package be.florien.ampacheplayer.persistence.local.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

sealed class Filter (
    var clause: String){

    open class StringFilter(clause: String, var argument: String) : Filter(clause)

    open class LongFilter(clause: String, var argument: Long) : Filter(clause)

    /**
     * String filters
     */
    class TitleIs(argument: String) : StringFilter(TITLE_IS, argument)

    class TitleContain(argument: String) : StringFilter(TITLE_CONTAIN, argument)

    class GenreIs(argument: String) : StringFilter(GENRE_IS, argument)

    /**
     * Long filters
     */

    class SongIs(argument: Long) : LongFilter(SONG_ID, argument)

    class ArtistIs(argument: Long) : LongFilter(ARTIST_ID, argument)

    class AlbumArtistIs(argument: Long) : LongFilter(ALBUM_ARTIST_ID, argument)

    class AlbumIs(argument: Long) : LongFilter(ALBUM_ID, argument)

    companion object {
        private const val TITLE_IS = "title = "
        private const val TITLE_CONTAIN = "title LIKE "
        private const val GENRE_IS = "song.genre = "
        private const val SONG_ID = "song.id = "
        private const val ARTIST_ID = "song.artistId = "
        private const val ALBUM_ARTIST_ID = "song.albumArtistId = "
        private const val ALBUM_ID = "song.albumId = "

        fun getTypedFilter(fromDb: DbFilter): Filter {
            return when (fromDb.clause) {
                TITLE_IS-> TitleIs(fromDb.argument)
                TITLE_CONTAIN-> TitleContain(fromDb.argument)
                GENRE_IS-> GenreIs(fromDb.argument)
                SONG_ID-> SongIs(fromDb.argument.toLong())
                ARTIST_ID-> ArtistIs(fromDb.argument.toLong())
                ALBUM_ARTIST_ID-> AlbumArtistIs(fromDb.argument.toLong())
                ALBUM_ID-> AlbumIs(fromDb.argument.toLong())
                else -> TitleIs("")
            }
        }

        fun toDbFilter(fromApp: Filter): DbFilter {
            return when(fromApp) {
                is StringFilter -> DbFilter().apply {
                    clause = fromApp.clause
                    argument = fromApp.argument
                }
                is LongFilter -> DbFilter().apply {
                    clause = fromApp.clause
                    argument = fromApp.argument.toString()
                }
            }
        }
    }

    @Entity
    class DbFilter {
        @PrimaryKey(autoGenerate = true)
        var id: Long = 0
        var clause: String = ""
        var argument: String = ""
    }
}