package be.florien.anyflow.player

import be.florien.anyflow.data.local.model.DbFilter
import be.florien.anyflow.data.local.model.FilterGroup

sealed class Filter<T>(
        val clause: String,
        val argument: T,
        val displayText: String,
        val displayImage: String? = null) {

    fun toDbFilter(group: FilterGroup): DbFilter = DbFilter(0, clause, argument.toString(), displayText, displayImage, group.id)

    override fun equals(other: Any?): Boolean {
        return other is Filter<*> && clause == other.clause && argument == other.argument
    }

    override fun hashCode(): Int {
        var result = clause.hashCode()
        result = 31 * result + (argument?.hashCode() ?: 0)
        return result
    }

    /**
     * String filters
     */
    class TitleIs(argument: String) : Filter<String>(TITLE_IS, argument, argument)

    class TitleContain(argument: String) : Filter<String>(TITLE_CONTAIN, argument, argument)

    class GenreIs(argument: String) : Filter<String>(GENRE_IS, argument, argument)

    class Search(argument: String) : Filter<String>(SEARCH, argument, argument)

    /**
     * Long filters
     */

    class SongIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(SONG_ID, argument, displayValue, displayImage)

    class ArtistIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(ARTIST_ID, argument, displayValue, displayImage)

    class AlbumArtistIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(ALBUM_ARTIST_ID, argument, displayValue, displayImage)

    class AlbumIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(ALBUM_ID, argument, displayValue, displayImage)

    companion object {
        private const val TITLE_IS = "title ="
        private const val TITLE_CONTAIN = "title LIKE"
        private const val SEARCH = "title AND genre AND artistName AND albumName LIKE"
        private const val GENRE_IS = "song.genre LIKE"
        private const val SONG_ID = "song.id ="
        private const val ARTIST_ID = "song.artistId ="
        private const val ALBUM_ARTIST_ID = "song.albumArtistId ="
        private const val ALBUM_ID = "song.albumId ="

        fun toTypedFilter(fromDb: DbFilter): Filter<*> {
            return when (fromDb.clause) {
                TITLE_IS -> TitleIs(fromDb.argument)
                TITLE_CONTAIN -> TitleContain(fromDb.argument)
                GENRE_IS -> GenreIs(fromDb.argument)
                SONG_ID -> SongIs(fromDb.argument.toLong(), fromDb.displayText, fromDb.displayImage)
                ARTIST_ID -> ArtistIs(fromDb.argument.toLong(), fromDb.displayText, fromDb.displayImage)
                ALBUM_ARTIST_ID -> AlbumArtistIs(fromDb.argument.toLong(), fromDb.displayText, fromDb.displayImage)
                ALBUM_ID -> AlbumIs(fromDb.argument.toLong(), fromDb.displayText, fromDb.displayImage)
                else -> TitleIs("")
            }
        }
    }
}