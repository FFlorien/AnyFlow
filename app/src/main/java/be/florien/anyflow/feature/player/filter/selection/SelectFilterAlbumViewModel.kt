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
    override fun getPagingList(filters: List<Filter<*>>?, search: String?) =
        dataRepository.getAlbums(::convert, filters, search)

    override fun isThisTypeOfFilter(filter: Filter<*>) = filter.type == Filter.FilterType.ALBUM_IS

    override suspend fun getFoundFilters(
        filters: List<Filter<*>>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getAlbumsSearchedList(filters, search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(
            Filter(
                Filter.FilterType.ALBUM_IS,
                filterValue.id,
                filterValue.displayName,
                emptyList()
            )
        )

    private fun convert(album: DbAlbumDisplay): FilterItem {
        val artUrl = dataRepository.getAlbumArtUrl(album.album.id)
        val filter =
            getFilterInParent(
                Filter(
                    Filter.FilterType.ALBUM_IS,
                    album.album.id,
                    album.album.name,
                    emptyList()
                )
            )
        return FilterItem(
            album.album.id,
            "${album.album.name}\nby ${album.artist.name}",
            filtersManager.isFilterInEdition(filter),
            artUrl
        )
    }
}