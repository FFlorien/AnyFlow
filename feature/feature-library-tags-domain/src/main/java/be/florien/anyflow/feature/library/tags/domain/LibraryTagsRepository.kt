package be.florien.anyflow.feature.library.tags.domain

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.common.management.convertToPagingLiveData
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.playlist.PlaylistRepository
import be.florien.anyflow.management.playlist.model.Playlist
import be.florien.anyflow.tags.DataRepository
import be.florien.anyflow.tags.UrlRepository
import be.florien.anyflow.tags.model.Album
import be.florien.anyflow.tags.model.Artist
import be.florien.anyflow.tags.model.Genre
import be.florien.anyflow.tags.model.SongDisplay
import javax.inject.Inject

@ServerScope
class LibraryTagsRepository @Inject constructor(
    private val dataRepository: DataRepository,
    private val playlistRepository: PlaylistRepository,
    private val urlRepository: UrlRepository,
    private val filtersManager: FiltersManager
) {
    // region paging
    fun getSongFiltersPaging(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        dataRepository
            .getSongs(filter?.let { listOf(it) }, search)
            .map { it.toFilterItem(filter, urlRepository, filtersManager) }
            .convertToPagingLiveData()

    fun getArtistFiltersPaging(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        dataRepository
            .getArtists(filter?.let { listOf(it) }, search)
            .map { it.toFilterItem(filter, urlRepository, filtersManager) }
            .convertToPagingLiveData()

    fun getAlbumFiltersPaging(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        dataRepository
            .getAlbums(filter?.let { listOf(it) }, search)
            .map { it.toFilterItem(filter, urlRepository, filtersManager) }
            .convertToPagingLiveData()

    fun getAlbumArtistsPaging(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        dataRepository
            .getAlbumArtists(filter?.let { listOf(it) }, search)
            .map { it.toFilterItem(filter, urlRepository, filtersManager) }
            .convertToPagingLiveData()

    fun getGenreFiltersPaging(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        dataRepository
            .getGenres(filter?.let { listOf(it) }, search)
            .map { it.toFilterItem(filter, filtersManager) }
            .convertToPagingLiveData()

    fun getPlaylistFiltersPaging(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        playlistRepository
            .getPlaylists(filter?.let { listOf(it) }, search)
            .map { it.toFilterItem(filter, urlRepository, filtersManager) }
            .convertToPagingLiveData()
    //endregion

    //region Filter list
    suspend fun getSongFilterList(
        filter: Filter<*>?,
        search: String
    ) = dataRepository
        .getSongsSearchedList(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }

    suspend fun getArtistFilterList(
        filter: Filter<*>?,
        search: String
    ) = dataRepository
        .getArtistsSearchedList(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }

    suspend fun getAlbumFilterList(
        filter: Filter<*>?,
        search: String
    ) = dataRepository
        .getAlbumsSearchedList(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }

    suspend fun getAlbumArtistFilterList(
        filter: Filter<*>?,
        search: String
    ) = dataRepository
        .getAlbumArtistsSearchedList(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }

    suspend fun getGenreFilterList(
        filter: Filter<*>?,
        search: String
    ) = dataRepository
        .getGenresSearchedList(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, filtersManager) }

    suspend fun getPlaylistFilterList(
        filter: Filter<*>?,
        search: String
    ) = playlistRepository
        .getPlaylistsSearchedList(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }
    //endregion

    //region Display list
    suspend fun getSongList(filter: Filter<*>?) =
        dataRepository
            .getSongsSearchedList(filter?.let { listOf(it) }, "")
            .map(SongDisplay::toDisplayData)

    suspend fun getArtistList(filter: Filter<*>?) =
        dataRepository
            .getArtistsSearchedList(filter?.let { listOf(it) }, "")
            .map(Artist::toDisplayData)

    suspend fun getAlbumList(filter: Filter<*>?) =
        dataRepository
            .getAlbumsSearchedList(filter?.let { listOf(it) }, "")
            .map(Album::toDisplayData)

    suspend fun getAlbumArtistList(filter: Filter<*>?) =
        dataRepository
            .getAlbumArtistsSearchedList(filter?.let { listOf(it) }, "")
            .map(Artist::toDisplayData)

    suspend fun getGenreList(filter: Filter<*>?) =
        dataRepository
            .getGenresSearchedList(filter?.let { listOf(it) }, "")
            .map(Genre::toDisplayData)

    suspend fun getPlaylistList(filter: Filter<*>?) =
        playlistRepository
            .getPlaylistsSearchedList(filter?.let { listOf(it) }, "")
            .map(Playlist::toDisplayData)
    //endregion

    suspend fun getFilteredInfo(infoSource: Filter<*>?) = dataRepository.getFilteredInfo(infoSource)

    fun getArtUrl(artType: String?, argument: Long): String? =
        if (artType == null) {
            null
        } else {
            urlRepository.getArtUrl(artType, argument)
        }
}