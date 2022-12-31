package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbAlbumDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.FilterActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SelectFilterAlbumViewModel @Inject constructor(
    val dataRepository: DataRepository,
    filterActions: FilterActions
) : SelectFilterViewModel(filterActions) {
    override fun getUnfilteredPagingList() = dataRepository.getAlbums(::convert)
    override fun getSearchedPagingList(search: String) =
        dataRepository.getAlbumsSearched(search, ::convert)

    override fun getFilteredPagingList(filters: List<Filter<*>>): LiveData<PagingData<FilterItem>> = dataRepository.getAlbumsFiltered(filters, ::convert)

    override fun isThisTypeOfFilter(filter: Filter<*>) = filter.type == Filter.FilterType.ALBUM_IS

    override suspend fun getFoundFilters(search: String): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getAlbumsSearchedList(search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) = Filter(Filter.FilterType.ALBUM_IS, filterValue.id, filterValue.displayName, emptyList())

    private fun convert(album: DbAlbumDisplay): FilterItem {
        val artUrl = dataRepository.getAlbumArtUrl(album.album.id)
        return FilterItem(
            album.album.id,
            "${album.album.name}\nby ${album.artist.name}",
            artUrl,
            filtersManager.isFilterInEdition(Filter(Filter.FilterType.ALBUM_IS, album.album.id, album.album.name, emptyList()))
        )
    }
}