package be.florien.anyflow.feature.player.ui.library.list.viewmodels

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.local.model.DbPlaylistWithCount
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.services.queue.FiltersManager
import be.florien.anyflow.feature.player.ui.library.list.LibraryListViewModel
import be.florien.anyflow.feature.playlist.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryPlaylistListViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val urlRepository: UrlRepository,
    filtersManager: FiltersManager
) : LibraryListViewModel(filtersManager) {
    override fun getPagingList(
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        playlistRepository.getPlaylists(::convert, filters, search)

    override fun isThisTypeOfFilter(filter: Filter<*>) =
        filter.type == Filter.FilterType.PLAYLIST_IS

    override suspend fun getFoundFilters(
        filters: List<Filter<*>>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            playlistRepository.getPlaylistsSearchedList(filters, search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(
            Filter(
                Filter.FilterType.PLAYLIST_IS,
                filterValue.id,
                filterValue.displayName
            )
        )

    private fun convert(playlist: DbPlaylistWithCount): FilterItem {
        val artUrl = urlRepository.getPlaylistArtUrl(playlist.id)
        val filter =
            getFilterInParent(Filter(Filter.FilterType.PLAYLIST_IS, playlist.id, playlist.name))
        return FilterItem(
            playlist.id,
            playlist.name,
            filtersManager.isFilterInEdition(filter),
            artUrl
        )
    }
}