package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterGenreViewModel @Inject constructor(private val dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    override val itemDisplayType = ITEM_LIST

    override fun getUnfilteredPagingList() = dataRepository.getGenres(::convert)
    override fun getFilteredPagingList(search: String) = dataRepository.getGenresFiltered(search, ::convert)
    override suspend fun getFoundFilters(search: String): List<FilterItem> = dataRepository.getGenresFilteredList(search, ::convert)

    override fun getFilter(filterValue: FilterItem) = Filter.GenreIs(filterValue.displayName)
    private var currentId = 0L

    private fun convert(genre: String) = FilterItem(currentId++, genre, isSelected = filtersManager.isFilterInEdition(Filter.GenreIs(genre)))
}