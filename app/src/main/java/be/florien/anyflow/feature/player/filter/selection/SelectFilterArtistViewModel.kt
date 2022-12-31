package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbArtist
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.FilterActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SelectFilterArtistViewModel @Inject constructor(
    val dataRepository: DataRepository,
    filterActions: FilterActions
) : SelectFilterViewModel(filterActions) {
    override fun getUnfilteredPagingList() = dataRepository.getAlbumArtists(::convert)
    override fun getSearchedPagingList(search: String) =
        dataRepository.getAlbumArtistsSearched(search, ::convert)

    override fun getFilteredPagingList(filters: List<Filter<*>>): LiveData<PagingData<FilterItem>> = dataRepository.getAlbumArtistsFiltered(filters, ::convert)

    override fun isThisTypeOfFilter(filter: Filter<*>) = filter.type == Filter.FilterType.ALBUM_ARTIST_IS

    override suspend fun getFoundFilters(search: String): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getAlbumArtistsSearchedList(search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        Filter(Filter.FilterType.ALBUM_ARTIST_IS, filterValue.id, filterValue.displayName)

    private fun convert(artist: DbArtist): FilterItem {
        val artUrl = dataRepository.getArtistArtUrl(artist.id)
        return FilterItem(
            artist.id, artist.name,
            artUrl, filtersManager.isFilterInEdition(Filter(Filter.FilterType.ALBUM_ARTIST_IS, artist.id, artist.name))
        )
    }
}