package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbAlbumDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterAlbumViewModel @Inject constructor(val dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {

    override val itemDisplayType = ITEM_GRID

    override fun getUnfilteredPagingList() = dataRepository.getAlbums(::convert)
    override fun getFilteredPagingList(search: String) = dataRepository.getAlbumsFiltered(search, ::convert)
    override suspend fun getFoundFilters(search: String): List<FilterItem> = dataRepository.getAlbumsFilteredList(search, ::convert)

    override fun getFilter(filterValue: FilterItem) = Filter.AlbumIs(filterValue.id, filterValue.displayName)

    private fun convert(album: DbAlbumDisplay): FilterItem {
        val artUrl = dataRepository.getAlbumArtUrl(album.album.id)
        return FilterItem(album.album.id, "${album.album.name}\nby ${album.artist.name}",
            artUrl, filtersManager.isFilterInEdition(Filter.AlbumIs(album.album.id, album.album.name)))
    }
}