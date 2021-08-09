package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbAlbumDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterAlbumViewModel @Inject constructor(val dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {

    override fun getUnfilteredPagingList() = dataRepository.getAlbums(::convert)

    override fun getFilteredPagingList(search: String) = dataRepository.getAlbumsFiltered(search, ::convert)

    override val itemDisplayType = ITEM_GRID

    override fun getFilter(filterValue: FilterItem) = Filter.AlbumIs(filterValue.id, filterValue.displayName, filterValue.artUrl)
    override suspend fun getFoundFilters(search: String): List<FilterItem> = dataRepository.getAlbumsFilteredList(search, ::convert)

    private fun convert(album: DbAlbumDisplay) =
            FilterItem(album.id, "${album.name}\nby ${album.artistName}", album.art, filtersManager.isFilterInEdition(Filter.AlbumIs(album.id, album.name, album.art)))

}