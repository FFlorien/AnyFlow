package be.florien.anyflow.feature.library.tags.domain

import androidx.paging.PagingData
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.management.convertToPagingFlow
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.playlist.PlaylistRepository
import be.florien.anyflow.tags.TagsRepository
import be.florien.anyflow.urls.UrlRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@ServerScope
class LibraryListTagsRepository @Inject constructor(
    private val tagsRepository: TagsRepository,
    private val playlistRepository: PlaylistRepository,
    private val urlRepository: UrlRepository,
    private val filtersManager: FiltersManager
) {
    // region paging
    fun getSongFiltersPaging(
        filter: Filter?,
        search: String?
    ): Flow<PagingData<FilterItem>> =
        tagsRepository
            .getSongs(filter, search)
            .map { it.toFilterItem(filter?.clone() as Filter?, urlRepository, filtersManager) }
            .convertToPagingFlow()

    fun getArtistFiltersPaging(
        filter: Filter?,
        search: String?
    ): Flow<PagingData<FilterItem>> =
        tagsRepository
            .getArtists(filter, search)
            .map { it.toFilterItem(filter?.clone() as Filter?, urlRepository, filtersManager) }
            .convertToPagingFlow()

    fun getAlbumFiltersPaging(
        filter: Filter?,
        search: String?
    ): Flow<PagingData<FilterItem>> =
        tagsRepository
            .getAlbums(filter, search)
            .map { it.toFilterItem(filter?.clone() as Filter?, urlRepository, filtersManager) }
            .convertToPagingFlow()

    fun getAlbumArtistsPaging(
        filter: Filter?,
        search: String?
    ): Flow<PagingData<FilterItem>> =
        tagsRepository
            .getAlbumArtists(filter, search)
            .map { it.toFilterItem(filter?.clone() as Filter?, urlRepository, filtersManager) }
            .convertToPagingFlow()

    fun getGenreFiltersPaging(
        filter: Filter?,
        search: String?
    ): Flow<PagingData<FilterItem>> =
        tagsRepository
            .getGenres(filter, search)
            .map { it.toFilterItem(filter?.clone() as Filter?, filtersManager) }
            .convertToPagingFlow()

    fun getPlaylistFiltersPaging(
        filter: Filter?,
        search: String?
    ): Flow<PagingData<FilterItem>> =
        playlistRepository
            .getPlaylists(filter, search)
            .map { it.toFilterItem(filter?.clone() as Filter?, urlRepository, filtersManager) }
            .convertToPagingFlow()

    fun getDownloadedFiltersPaging(
        filter: Filter?,
        isDownloadedName: String,
        isNotDownloadedName: String
    ): Flow<PagingData<FilterItem>> =
        tagsRepository
            .getDownloadedInfo(filter)
            .map { it.toFilterItem(filter?.clone() as Filter?, filtersManager, isDownloadedName, isNotDownloadedName)}
            .convertToPagingFlow()
    //endregion

    //region Filter list
    suspend fun getSongFilterList(
        filter: Filter?,
        search: String
    ) = tagsRepository
        .getSongFiltered(filter, search)
        .map { it.toFilterItem(filter?.clone() as Filter?, urlRepository, filtersManager) }

    suspend fun getArtistFilterList(
        filter: Filter?,
        search: String
    ) = tagsRepository
        .getArtistFiltered(filter, search)
        .map { it.toFilterItem(filter?.clone() as Filter?, urlRepository, filtersManager) }

    suspend fun getAlbumFilterList(
        filter: Filter?,
        search: String
    ) = tagsRepository
        .getAlbumFiltered(filter, search)
        .map { it.toFilterItem(filter?.clone() as Filter?, urlRepository, filtersManager) }

    suspend fun getAlbumArtistFilterList(
        filter: Filter?,
        search: String
    ) = tagsRepository
        .getAlbumArtistFiltered(filter, search)
        .map { it.toFilterItem(filter?.clone() as Filter?, urlRepository, filtersManager) }

    suspend fun getGenreFilterList(
        filter: Filter?,
        search: String
    ) = tagsRepository
        .getGenreFiltered(filter, search)
        .map { it.toFilterItem(filter?.clone() as Filter?, filtersManager) }

    suspend fun getPlaylistFilterList(
        filter: Filter?,
        search: String
    ) = playlistRepository
        .getPlaylistFiltered(filter, search)
        .map { it.toFilterItem(filter?.clone() as Filter?, urlRepository, filtersManager) }

    suspend fun getDownloadedFiltersList(
        filter: Filter?,
        isDownloadedName: String,
        isNotDownloadedName: String
    ): List<FilterItem> =
        tagsRepository
            .getDownloadedSearchedList(filter)
            .map { it.toFilterItem(filter?.clone() as Filter?, filtersManager, isDownloadedName, isNotDownloadedName)}
    //endregion
}