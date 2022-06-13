package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbSongDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterSongViewModel @Inject constructor(val dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {

    override val itemDisplayType = ITEM_LIST

    override fun getUnfilteredPagingList() = dataRepository.getSongs(::convert)
    override fun getFilteredPagingList(search: String) = dataRepository.getSongsFiltered(search, ::convert)
    override suspend fun getFoundFilters(search: String): List<FilterItem> = dataRepository.getSongsFilteredList(search, ::convert)

    override fun getFilter(filterValue: FilterItem) = Filter.SongIs(filterValue.id, filterValue.displayName, filterValue.artUrl)

    private fun convert(song: DbSongDisplay): FilterItem {
        val artUrl = dataRepository.getAlbumArtUrl(song.album.album.id)
        return FilterItem(song.song.id, "${song.song.title}\nby ${song.artist.name}\nfrom ${song.album.album.name}",
            artUrl, filtersManager.isFilterInEdition(Filter.SongIs(song.song.id, song.song.title, artUrl)))
    }
}