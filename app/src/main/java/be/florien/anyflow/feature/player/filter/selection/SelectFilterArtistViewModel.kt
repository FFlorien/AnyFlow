package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbArtistDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterArtistViewModel @Inject constructor(val dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    override val itemDisplayType = ITEM_LIST

    override fun getUnfilteredPagingList() = dataRepository.getArtists(::convert)
    override fun getFilteredPagingList(search: String) = dataRepository.getArtistsFiltered(search, ::convert)
    override suspend fun getFoundFilters(search: String): List<FilterItem> = dataRepository.getArtistsFilteredList(search, ::convert)

    override fun getFilter(filterValue: FilterItem) = Filter.ArtistIs(filterValue.id, filterValue.displayName, filterValue.artUrl)

    private fun convert(artist: DbArtistDisplay) = FilterItem(artist.id, artist.name, artist.art, filtersManager.isFilterInEdition(Filter.ArtistIs(artist.id, artist.name, artist.art)))
}