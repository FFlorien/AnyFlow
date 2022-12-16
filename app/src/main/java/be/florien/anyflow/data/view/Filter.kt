package be.florien.anyflow.data.view

import be.florien.anyflow.data.DataRepository
import kotlin.time.Duration

sealed class Filter<T>(
        val argument: T,
        val displayText: String,
        val imageType: String? = null,
        val childrenFilters: List<Filter<*>>
) {


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
    class TitleIs(argument: String, childFilters: List<Filter<*>> = emptyList()) : Filter<String>(argument, argument, childrenFilters = childFilters)

    class TitleContain(argument: String, childFilters: List<Filter<*>> = emptyList()) : Filter<String>(argument, argument, childrenFilters = childFilters)

    class Search(argument: String, childFilters: List<Filter<*>> = emptyList()) : Filter<String>(argument, argument, childrenFilters = childFilters)

    /**
     * Long filters
     */

    class SongIs(argument: Long, displayValue: String, childFilters: List<Filter<*>> = emptyList()) : Filter<Long>(argument, displayValue, DataRepository.ART_TYPE_SONG, childrenFilters = childFilters)

    class ArtistIs(argument: Long, displayValue: String, childFilters: List<Filter<*>> = emptyList()) : Filter<Long>(argument, displayValue, DataRepository.ART_TYPE_ARTIST, childrenFilters = childFilters)

    class AlbumArtistIs(argument: Long, displayValue: String, childFilters: List<Filter<*>> = emptyList()) : Filter<Long>(argument, displayValue, DataRepository.ART_TYPE_ARTIST, childrenFilters = childFilters)

    class AlbumIs(argument: Long, displayValue: String, childFilters: List<Filter<*>> = emptyList()) : Filter<Long>(argument, displayValue, DataRepository.ART_TYPE_ALBUM, childrenFilters = childFilters)

    class GenreIs(argument: Long, name: String, childFilters: List<Filter<*>> = emptyList()) : Filter<Long>(argument, name, childrenFilters = childFilters)

    class PlaylistIs(argument: Long, displayValue: String, childFilters: List<Filter<*>> = emptyList()) : Filter<Long>(argument, displayValue, DataRepository.ART_TYPE_PLAYLIST, childrenFilters = childFilters)

    /**
     * Boolean filters
     */

    class DownloadedStatusIs(argument: Boolean, childFilters: List<Filter<*>> = emptyList()) : Filter<Boolean>(argument, argument.toString(), childrenFilters = childFilters)
}

data class FilterCount(
    val duration: Duration,
    val genres: Int?,
    val albumArtists: Int?,
    val albums: Int?,
    val artists: Int?,
    val songs: Int?,
    val playlists: Int?
)