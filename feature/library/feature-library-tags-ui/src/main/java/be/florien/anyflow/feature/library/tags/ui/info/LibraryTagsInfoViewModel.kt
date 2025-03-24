package be.florien.anyflow.feature.library.tags.ui.info

import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.library.tags.domain.LibraryInfoRow
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsActionType
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsFieldType
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsRepository
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryTagsInfoViewModel @Inject constructor(
    val libraryTagsRepository: LibraryTagsRepository, //todo this doesn't get injected
    filtersManager: FiltersManager,
    navigator: Navigator
) : LibraryInfoViewModel<LibraryInfoRow, TagFilterType>(filtersManager, navigator), LibraryViewModel {
    override fun getArtUrl(artType: String, id: Long): String? =
        libraryTagsRepository.getArtUrl(artType, id)

    override suspend fun getInfoRowList(): MutableList<LibraryInfoRow> {
        val filteredInfo =
            withContext(Dispatchers.IO) { libraryTagsRepository.getFilteredInfo(filterNavigation) }
        return mutableListOf(
            LibraryInfoRow(
                LibraryTagsFieldType.Duration,
                LibraryTagsActionType.InfoTitle,
                filteredInfo.duration
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Genre,
                getAction(filteredInfo.genres.minus(filterNavigation?.let { source ->
                    val genreFilters = mutableSetOf<Long>()
                    source.forEach { filter ->
                        if (filter.type == TagFilterType.GENRE_IS) {
                            genreFilters.add(filter.argument as Long)
                        }
                    }
                    genreFilters.size
                } ?: 0)), // todo "Electro and 3 other genres" instead
                filteredInfo.genres
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.AlbumArtist,
                getAction(filteredInfo.albumArtists),
                filteredInfo.albumArtists
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Album,
                getAction(filteredInfo.albums),
                filteredInfo.albums
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Artist,
                getAction(filteredInfo.artists),
                filteredInfo.artists
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Song,
                getAction(filteredInfo.songs),
                filteredInfo.songs
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Downloaded,
                LibraryTagsActionType.SubFilter,
                filteredInfo.downloaded
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Playlist,
                getAction(filteredInfo.playlists.minus(filterNavigation?.let { source ->
                    val playlistFilters = mutableSetOf<Long>()
                    source.forEach { filter ->
                        if (filter.type == TagFilterType.PLAYLIST_IS) {
                            playlistFilters.add(filter.argument as Long)
                        }
                    }
                    playlistFilters.size
                } ?: 0)), // todo "Motivation and 3 other playlists" instead
                filteredInfo.playlists
            )
        )
    }

    private fun getAction(count: Int): LibraryTagsActionType {
        return if (count > 1) LibraryTagsActionType.SubFilter else LibraryTagsActionType.InfoTitle
    }

    override suspend fun getFilteredInfo(
        filterType: TagFilterType,
        filter: Filter?
    ) = when (filterType) {
        TagFilterType.SONG_IS -> libraryTagsRepository.getSongFiltered(filter)
        TagFilterType.ARTIST_IS -> libraryTagsRepository.getArtistFiltered(filter)
        TagFilterType.ALBUM_ARTIST_IS -> libraryTagsRepository.getAlbumArtistFiltered(
            filter
        )

        TagFilterType.ALBUM_IS -> libraryTagsRepository.getAlbumFiltered(filter)
        TagFilterType.GENRE_IS -> libraryTagsRepository.getGenreFiltered(filter)
        TagFilterType.PLAYLIST_IS -> libraryTagsRepository.getPlaylistFiltered(filter)
        TagFilterType.DOWNLOADED_STATUS_IS,
        TagFilterType.DISK_IS -> listOf(null)

    }.firstOrNull()

    companion object {
        const val GENRE_ID = "Genre"
        const val ALBUM_ARTIST_ID = "AlbumArtist"
        const val ARTIST_ID = "Artist"
        const val ALBUM_ID = "Album"
        const val SONG_ID = "Song"
        const val PLAYLIST_ID = "Playlist"
        const val DOWNLOAD_ID = "Download"
    }
}