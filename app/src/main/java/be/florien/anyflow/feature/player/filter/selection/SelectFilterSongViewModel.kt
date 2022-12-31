package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbSongDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.FilterActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SelectFilterSongViewModel @Inject constructor(
    val dataRepository: DataRepository,
    filterActions: FilterActions
) : SelectFilterViewModel(filterActions) {
    override fun getUnfilteredPagingList() = dataRepository.getSongs(::convert)
    override fun getSearchedPagingList(search: String) =
        dataRepository.getSongsSearched(search, ::convert)

    override fun getFilteredPagingList(filters: List<Filter<*>>): LiveData<PagingData<FilterItem>> = dataRepository.getSongsFiltered(filters, ::convert)


    override fun isThisTypeOfFilter(filter: Filter<*>): Boolean  = filter.type == Filter.FilterType.SONG_IS

    override suspend fun getFoundFilters(search: String): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getSongsSearchedList(search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        Filter(Filter.FilterType.SONG_IS, filterValue.id, filterValue.displayName)

    private fun convert(song: DbSongDisplay): FilterItem {
        val artUrl = dataRepository.getAlbumArtUrl(song.album.album.id)
        return FilterItem(
            song.song.id,
            "${song.song.title}\nby ${song.artist.name}\nfrom ${song.album.album.name}", //todo wut ? i18n ?
            artUrl,
            filtersManager.isFilterInEdition(Filter(Filter.FilterType.SONG_IS, song.song.id, song.song.title))
        )
    }
}