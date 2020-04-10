package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterArtistViewModel @Inject constructor(dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    override val values: LiveData<PagedList<FilterItem>> =
            dataRepository.getArtists { artist -> FilterItem(artist.id, artist.name, artist.art, filtersManager.isFilterInEdition(Filter.ArtistIs(artist.id, artist.name, artist.art))) }
    override val itemDisplayType = ITEM_LIST

    override fun getFilter(filterValue: FilterItem) = Filter.ArtistIs(filterValue.id, filterValue.displayName, filterValue.artUrl)
}