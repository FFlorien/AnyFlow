package be.florien.anyflow.data.view

import be.florien.anyflow.data.DataRepository

sealed class Filter<T>(
        val argument: T,
        val displayText: String,
        val displayImage: String? = null) {

    suspend fun contains(song: SongInfo, dataRepository: DataRepository): Boolean {
        return when (this) {
            is TitleIs -> song.title == argument
            is AlbumArtistIs -> song.albumArtistId == argument
            is AlbumIs -> song.albumId == argument
            is ArtistIs -> song.artistId == argument
            is GenreIs -> song.genreIds.any{ it == argument }
            is Search -> song.title.contains(argument, ignoreCase = true) || song.artistName.contains(argument, ignoreCase = true) || song.albumName.contains(argument, ignoreCase = true) || song.genreNames.any { it.contains(argument, ignoreCase = true) }
            is SongIs -> song.id == argument
            is TitleContain -> song.title.contains(argument, ignoreCase = true)
            is PlaylistIs -> dataRepository.isPlaylistContainingSong(argument, song.id)
            is DownloadedStatusIs -> dataRepository.hasDownloaded(song)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is Filter<*> && argument == other.argument
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

    class Search(argument: String) : Filter<String>(argument, argument)

    /**
     * Long filters
     */

    class SongIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(argument, displayValue, displayImage)

    class ArtistIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(argument, displayValue, displayImage)

    class AlbumArtistIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(argument, displayValue, displayImage)

    class AlbumIs(argument: Long, displayValue: String, displayImage: String?) : Filter<Long>(argument, displayValue, displayImage)

    class GenreIs(argument: Long, name: String) : Filter<Long>(argument, name)

    class PlaylistIs(argument: Long, displayValue: String) : Filter<Long>(argument, displayValue, null)

    /**
     * Boolean filters
     */

    class DownloadedStatusIs(argument: Boolean) : Filter<Boolean>(argument, argument.toString())
}