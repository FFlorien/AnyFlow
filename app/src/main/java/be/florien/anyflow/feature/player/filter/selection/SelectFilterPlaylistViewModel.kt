package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbPlaylistWithCount
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.FilterActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SelectFilterPlaylistViewModel @Inject constructor(
    val dataRepository: DataRepository,
    filterActions: FilterActions
) : SelectFilterViewModel(filterActions) {
    override fun getPagingList(
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> = dataRepository.getPlaylists(::convert, filters, search)

    override fun isThisTypeOfFilter(filter: Filter<*>) = filter.type == Filter.FilterType.PLAYLIST_IS

    override suspend fun getFoundFilters(
        filters: List<Filter<*>>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getPlaylistsSearchedList(filters, search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(Filter(Filter.FilterType.PLAYLIST_IS, filterValue.id, filterValue.displayName))

    private fun convert(playlist: DbPlaylistWithCount): FilterItem {
        val artUrl = dataRepository.getPlaylistArtUrl(playlist.id)
        val filter = getFilterInParent(Filter(Filter.FilterType.PLAYLIST_IS, playlist.id, playlist.name))
        return FilterItem(
            playlist.id,
            playlist.name,
            filtersManager.isFilterInEdition(filter),
            artUrl
        )
    }
}