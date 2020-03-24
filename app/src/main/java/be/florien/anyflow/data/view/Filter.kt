package be.florien.anyflow.data.view

sealed class Filter<T>(
        val clause: String,
        val argument: T,
        val displayText: String,
        val displayImage: String? = null) {

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
        const val TITLE_IS = "title ="
        const val TITLE_CONTAIN = "title LIKE"
        const val SEARCH = "title AND genre AND artistName AND albumName LIKE"
        const val GENRE_IS = "song.genre LIKE"
        const val SONG_ID = "song.id ="
        const val ARTIST_ID = "song.artistId ="
        const val ALBUM_ARTIST_ID = "song.albumArtistId ="
        const val ALBUM_ID = "song.albumId ="
    }
}