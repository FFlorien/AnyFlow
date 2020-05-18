package be.florien.anyflow.data.view

sealed class Filter<T>(
        val argument: T,
        val displayText: String,
        val displayImage: String? = null) {

    override fun equals(other: Any?): Boolean {
        return other is Filter<*>  && argument == other.argument
    }

    override fun hashCode(): Int {
        var result = argument.hashCode() + javaClass.name.hashCode()
        result = 31 * result + (argument?.hashCode() ?: 0)
        return result
    }

    /**
     * String filters
     */
    class TitleIs(argument: String) : Filter<String>(argument, argument)

    class TitleContain(argument: String) : Filter<String>(argument, argument)

    class GenreIs(argument: String) : Filter<String>(argument, argument)

    class Search(argument: String) : Filter<String>(argument, argument)

    /**
     * Long filters
     */

    class SongIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(argument, displayValue, displayImage)

    class ArtistIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(argument, displayValue, displayImage)

    class AlbumArtistIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(argument, displayValue, displayImage)

    class AlbumIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(argument, displayValue, displayImage)
}