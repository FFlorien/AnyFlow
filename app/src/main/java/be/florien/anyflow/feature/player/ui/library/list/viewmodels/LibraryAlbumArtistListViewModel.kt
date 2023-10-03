package be.florien.anyflow.feature.player.ui.library.list.viewmodels

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.local.model.DbArtist
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.services.queue.FiltersManager
import be.florien.anyflow.feature.player.ui.library.list.LibraryListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryAlbumArtistListViewModel @Inject constructor(
    val dataRepository: DataRepository,
    val urlRepository: UrlRepository,
    filtersManager: FiltersManager
) : LibraryListViewModel(filtersManager) {
    override fun getPagingList(
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> = dataRepository.getAlbumArtists(::convert, filters, search)

    override fun isThisTypeOfFilter(filter: Filter<*>) =
        filter.type == Filter.FilterType.ALBUM_ARTIST_IS

    override suspend fun getFoundFilters(
        filters: List<Filter<*>>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getAlbumArtistsSearchedList(filters, search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem): Filter<*> {
        val filter =
            Filter(Filter.FilterType.ALBUM_ARTIST_IS, filterValue.id, filterValue.displayName)
        return getFilterInParent(filter)
    }

    private fun convert(artist: DbArtist): FilterItem {
        val artUrl = urlRepository.getArtistArtUrl(artist.id)
        val filter = getFilterInParent(
            Filter(
                Filter.FilterType.ALBUM_ARTIST_IS,
                artist.id,
                artist.name
            )
        )
        return FilterItem(
            artist.id,
            artist.name,
            filtersManager.isFilterInEdition(filter),
            artUrl
        )
    }
}