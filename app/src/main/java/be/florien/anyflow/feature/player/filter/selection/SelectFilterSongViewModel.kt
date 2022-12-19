package be.florien.anyflow.feature.player.filter.selection

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
    override fun getFilteredPagingList(search: String) =
        dataRepository.getSongsFiltered(search, ::convert)

    override fun isThisTypeOfFilter(filter: Filter<*>): Boolean  = filter is Filter.SongIs

    override suspend fun getFoundFilters(search: String): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getSongsFilteredList(search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        Filter.SongIs(filterValue.id, filterValue.displayName)

    private fun convert(song: DbSongDisplay): FilterItem {
        val artUrl = dataRepository.getAlbumArtUrl(song.album.album.id)
        return FilterItem(
            song.song.id,
            "${song.song.title}\nby ${song.artist.name}\nfrom ${song.album.album.name}",
            artUrl,
            filtersManager.isFilterInEdition(Filter.SongIs(song.song.id, song.song.title))
        )
    }
}