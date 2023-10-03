package be.florien.anyflow.feature.player.ui.library.list.viewmodels

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.local.model.DbSongDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.services.queue.FiltersManager
import be.florien.anyflow.feature.player.ui.library.list.LibraryListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibrarySongListViewModel @Inject constructor(
    val dataRepository: DataRepository,
    val urlRepository: UrlRepository,
    filtersManager: FiltersManager
) : LibraryListViewModel(filtersManager) {
    override fun getPagingList(
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> = dataRepository.getSongs(::convert, filters, search)

    override fun isThisTypeOfFilter(filter: Filter<*>): Boolean =
        filter.type == Filter.FilterType.SONG_IS

    override suspend fun getFoundFilters(
        filters: List<Filter<*>>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getSongsSearchedList(filters, search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(
            Filter(
                Filter.FilterType.SONG_IS,
                filterValue.id,
                filterValue.displayName
            )
        )

    private fun convert(song: DbSongDisplay): FilterItem {
        val artUrl = urlRepository.getAlbumArtUrl(song.albumId)
        val filter = getFilterInParent(Filter(Filter.FilterType.SONG_IS, song.id, song.title))
        return (FilterItem(
            song.id,
            "${song.title}\nby ${song.artistName}\nfrom ${song.albumName}", //todo wut ? i18n ?
            filtersManager.isFilterInEdition(filter),
            artUrl
        ))
    }
}