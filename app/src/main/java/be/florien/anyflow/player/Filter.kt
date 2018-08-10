package be.florien.anyflow.player

import be.florien.anyflow.persistence.local.model.DbFilter

sealed class Filter<T>(
        val clause: String,
        val argument: T,
        val displayValue: String) {

    fun toDbFilter(): DbFilter = DbFilter(0, clause, argument.toString(), displayValue)

    //open class StringFilter(clause: String, argument: String, displayValue: String) : Filter<String>(clause, argument, displayValue)

  //  open class LongFilter(clause: String, argument: Long, displayValue: String) : Filter<Long>(clause, argument, displayValue)

    /**
     * String filters
     */
    class TitleIs(argument: String) : Filter<String>(TITLE_IS, argument, argument)

    class TitleContain(argument: String) : Filter<String>(TITLE_CONTAIN, argument, argument)

    class GenreIs(argument: String) : Filter<String>(GENRE_IS, argument, argument)

    /**
     * Long filters
     */

    class SongIs(argument: Long, displayValue: String) : Filter<Long>(SONG_ID, argument, displayValue)

    class ArtistIs(argument: Long, displayValue: String) : Filter<Long>(ARTIST_ID, argument, displayValue)

    class AlbumArtistIs(argument: Long, displayValue: String) : Filter<Long>(ALBUM_ARTIST_ID, argument, displayValue)

    class AlbumIs(argument: Long, displayValue: String) : Filter<Long>(ALBUM_ID, argument, displayValue)

    companion object {
        private const val TITLE_IS = "title ="
        private const val TITLE_CONTAIN = "title LIKE"
        private const val GENRE_IS = "song.genre ="
        private const val SONG_ID = "song.id ="
        private const val ARTIST_ID = "song.artistId ="
        private const val ALBUM_ARTIST_ID = "song.albumArtistId ="
        private const val ALBUM_ID = "song.albumId ="

        fun toTypedFilter(fromDb: DbFilter): Filter<*> {
            return when (fromDb.clause) {
                TITLE_IS -> TitleIs(fromDb.argument)
                TITLE_CONTAIN -> TitleContain(fromDb.argument)
                GENRE_IS -> GenreIs(fromDb.argument)
                SONG_ID -> SongIs(fromDb.argument.toLong(), fromDb.displayValue)
                ARTIST_ID -> ArtistIs(fromDb.argument.toLong(), fromDb.displayValue)
                ALBUM_ARTIST_ID -> AlbumArtistIs(fromDb.argument.toLong(), fromDb.displayValue)
                ALBUM_ID -> AlbumIs(fromDb.argument.toLong(), fromDb.displayValue)
                else -> TitleIs("")
            }
        }
    }
}