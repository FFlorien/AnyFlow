package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbPlaylist
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterPlaylistViewModel @Inject constructor(val dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {

    override val itemDisplayType = ITEM_LIST

    override fun getUnfilteredPagingList() = dataRepository.getPlaylists(::convert)
    override fun getFilteredPagingList(search: String) = dataRepository.getPlaylistsFiltered(search, ::convert)
    override suspend fun getFoundFilters(search: String): List<FilterItem> = dataRepository.getPlaylistsFilteredList(search, ::convert)

    override fun getFilter(filterValue: FilterItem) = Filter.PlaylistIs(filterValue.id, filterValue.displayName)

    private fun convert(playlist: DbPlaylist) =
            FilterItem(playlist.id, playlist.name, null, filtersManager.isFilterInEdition(Filter.PlaylistIs(playlist.id, playlist.name)))
}