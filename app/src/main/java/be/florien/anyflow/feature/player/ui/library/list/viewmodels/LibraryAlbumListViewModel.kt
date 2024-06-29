package be.florien.anyflow.feature.player.ui.library.list.viewmodels

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.local.model.DbAlbumDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.services.queue.FiltersManager
import be.florien.anyflow.feature.player.ui.library.list.LibraryListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryAlbumListViewModel @Inject constructor(
    val dataRepository: DataRepository,
    val urlRepository: UrlRepository,
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

    private fun convert(album: DbAlbumDisplay): FilterItem {
        val artUrl = urlRepository.getAlbumArtUrl(album.albumId)
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