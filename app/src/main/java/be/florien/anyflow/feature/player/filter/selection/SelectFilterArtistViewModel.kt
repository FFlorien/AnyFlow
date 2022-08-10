package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbArtist
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterArtistViewModel @Inject constructor(val dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    override val itemDisplayType = ITEM_LIST

    override fun getUnfilteredPagingList() = dataRepository.getAlbumArtists(::convert)
    override fun getFilteredPagingList(search: String) = dataRepository.getAlbumArtistsFiltered(search, ::convert)
    override suspend fun getFoundFilters(search: String): List<FilterItem> = dataRepository.getAlbumArtistsFilteredList(search, ::convert)

    override fun getFilter(filterValue: FilterItem) = Filter.AlbumArtistIs(filterValue.id, filterValue.displayName)

    private fun convert(artist: DbArtist): FilterItem {
        val artUrl = dataRepository.getArtistArtUrl(artist.id)
        return FilterItem(artist.id, artist.name,
            artUrl, filtersManager.isFilterInEdition(Filter.AlbumArtistIs(artist.id, artist.name)))
    }
}