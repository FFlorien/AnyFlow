package be.florien.anyflow.feature.library.tags.domain

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.management.convertToPagingLiveData
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.playlist.PlaylistRepository
import be.florien.anyflow.management.playlist.model.PlaylistWithCount
import be.florien.anyflow.tags.DataRepository
import be.florien.anyflow.tags.UrlRepository
import be.florien.anyflow.tags.model.Album
import be.florien.anyflow.tags.model.Artist
import be.florien.anyflow.tags.model.Genre
import be.florien.anyflow.tags.model.SongDisplayDomain
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

    fun getDownloadedFiltersPaging(
        filter: Filter<*>?,
        isDownloadedName: String,
        isNotDownloadedName: String
    ): LiveData<PagingData<FilterItem>> =
        dataRepository
            .getDownloadedInfo(filter?.let { listOf(it) })
            .map { it.toFilterItem(filter, filtersManager, isDownloadedName, isNotDownloadedName)}
            .convertToPagingLiveData()
    //endregion

    //region Filter list
    suspend fun getSongFilterList(
        filter: Filter<*>?,
        search: String
    ) = dataRepository
        .getSongFiltered(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }

    suspend fun getArtistFilterList(
        filter: Filter<*>?,
        search: String
    ) = dataRepository
        .getArtistFiltered(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }

    suspend fun getAlbumFilterList(
        filter: Filter<*>?,
        search: String
    ) = dataRepository
        .getAlbumFiltered(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }

    suspend fun getAlbumArtistFilterList(
        filter: Filter<*>?,
        search: String
    ) = dataRepository
        .getAlbumArtistFiltered(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }

    suspend fun getGenreFilterList(
        filter: Filter<*>?,
        search: String
    ) = dataRepository
        .getGenreFiltered(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, filtersManager) }

    suspend fun getPlaylistFilterList(
        filter: Filter<*>?,
        search: String
    ) = playlistRepository
        .getPlaylistFiltered(filter?.let { listOf(it) }, search)
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }

    suspend fun getDownloadedFiltersList(
        filter: Filter<*>?,
        isDownloadedName: String,
        isNotDownloadedName: String
    ): List<FilterItem> =
        dataRepository
            .getDownloadedSearchedList(filter?.let { listOf(it) })
            .map { it.toFilterItem(filter, filtersManager, isDownloadedName, isNotDownloadedName)}
    //endregion

    //region Display list
    suspend fun getSongFiltered(filter: Filter<*>?) =
        dataRepository
            .getSongFiltered(filter?.let { listOf(it) }, "")
            .map(SongDisplayDomain::toIdText)

    suspend fun getArtistFiltered(filter: Filter<*>?) =
        dataRepository
            .getArtistFiltered(filter?.let { listOf(it) }, "")
            .map(Artist::toIdText)

    suspend fun getAlbumFiltered(filter: Filter<*>?) =
        dataRepository
            .getAlbumFiltered(filter?.let { listOf(it) }, "")
            .map(Album::toIdText)

    suspend fun getAlbumArtistFiltered(filter: Filter<*>?) =
        dataRepository
            .getAlbumArtistFiltered(filter?.let { listOf(it) }, "")
            .map(Artist::toIdText)

    suspend fun getGenreFiltered(filter: Filter<*>?) =
        dataRepository
            .getGenreFiltered(filter?.let { listOf(it) }, "")
            .map(Genre::toIdText)

    suspend fun getPlaylistFiltered(filter: Filter<*>?) =
        playlistRepository
            .getPlaylistFiltered(filter?.let { listOf(it) }, "")
            .map(PlaylistWithCount::toIdText)
    //endregion

    suspend fun getFilteredInfo(infoSource: Filter<*>?) = dataRepository.getFilteredInfo(infoSource)

    fun getArtUrl(artType: String?, argument: Long): String? =
        if (artType == null) {
            null
        } else {
            urlRepository.getArtUrl(artType, argument)
        }
}