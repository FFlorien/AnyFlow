package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterAlbumViewModel @Inject constructor(dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    override val values: LiveData<PagedList<FilterItem>> =
            dataRepository.getAlbums { album -> FilterItem(album.id, "${album.name}\nby ${album.artistName}", album.art, filtersManager.isFilterInEdition(Filter.AlbumIs(album.id, album.name, album.art))) }

    override val itemDisplayType = ITEM_GRID

    override fun getFilter(filterValue: FilterItem) = Filter.AlbumIs(filterValue.id, filterValue.displayName, filterValue.artUrl)
}