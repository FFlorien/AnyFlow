package be.florien.anyflow.feature.player.filter.selection

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

    override val itemDisplayType = ITEM_GRID

    override fun getUnfilteredPagingList() = dataRepository.getAlbums(::convert)
    override fun getFilteredPagingList(search: String) =
        dataRepository.getAlbumsFiltered(search, ::convert)
    override fun isThisTypeOfFilter(filter: Filter<*>) = filter is Filter.AlbumIs

    override suspend fun getFoundFilters(search: String): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getAlbumsFilteredList(search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        Filter.AlbumIs(filterValue.id, filterValue.displayName)

    private fun convert(album: DbAlbumDisplay): FilterItem {
        val artUrl = dataRepository.getAlbumArtUrl(album.album.id)
        return FilterItem(
            album.album.id,
            "${album.album.name}\nby ${album.artist.name}",
            artUrl,
            filtersManager.isFilterInEdition(Filter.AlbumIs(album.album.id, album.album.name))
        )
    }
}