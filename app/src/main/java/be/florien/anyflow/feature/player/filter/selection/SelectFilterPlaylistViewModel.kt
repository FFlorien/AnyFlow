package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbPlaylistWithCount
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SelectFilterPlaylistViewModel @Inject constructor(
    val dataRepository: DataRepository,
    filtersManager: FiltersManager
) : SelectFilterViewModel(filtersManager) {

    override val itemDisplayType = ITEM_LIST

    override fun getUnfilteredPagingList() = dataRepository.getPlaylists(::convert)
    override fun getFilteredPagingList(search: String) =
        dataRepository.getPlaylistsFiltered(search, ::convert)

    override fun isThisTypeOfFilter(filter: Filter<*>) = filter is Filter.PlaylistIs

    override suspend fun getFoundFilters(search: String): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getPlaylistsFilteredList(search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        Filter.PlaylistIs(filterValue.id, filterValue.displayName)

    private fun convert(playlist: DbPlaylistWithCount): FilterItem {
        val artUrl = dataRepository.getPlaylistArtUrl(playlist.id)
        return FilterItem(
            playlist.id,
            playlist.name,
            artUrl,
            filtersManager.isFilterInEdition(Filter.PlaylistIs(playlist.id, playlist.name))
        )
    }
}