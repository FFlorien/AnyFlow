package be.florien.anyflow.feature.player.library.list.viewmodels

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbAlbumDisplayForRaw
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.library.list.LibraryListViewModel
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryAlbumListViewModel @Inject constructor(
    val dataRepository: DataRepository,
    filtersManager: FiltersManager
) : LibraryListViewModel(filtersManager) {
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

    private fun convert(album: DbAlbumDisplayForRaw): FilterItem {
        val artUrl = dataRepository.getAlbumArtUrl(album.albumId)
        val filter =
            getFilterInParent(
                Filter(
                    Filter.FilterType.ALBUM_IS,
                    album.albumId,
                    album.albumName,
                    emptyList()
                )
            )
        return FilterItem(
            album.albumId,
            "${album.albumName}\nby ${album.albumArtistName}",
            filtersManager.isFilterInEdition(filter),
            artUrl
        )
    }
}