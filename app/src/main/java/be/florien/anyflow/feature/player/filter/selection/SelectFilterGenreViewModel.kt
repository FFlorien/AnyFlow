package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbGenre
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.FilterActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SelectFilterGenreViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    filterActions: FilterActions
) : SelectFilterViewModel(filterActions) {
    override fun getUnfilteredPagingList() = dataRepository.getGenres(::convert)
    override fun getFilteredPagingList(search: String) =
        dataRepository.getGenresFiltered(search, ::convert)

    override fun isThisTypeOfFilter(filter: Filter<*>)= filter is Filter.GenreIs

    override suspend fun getFoundFilters(search: String): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getGenresFilteredList(search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        Filter.GenreIs(filterValue.id, filterValue.displayName)

    private fun convert(genre: DbGenre) = FilterItem(
        genre.id,
        genre.name,
        isSelected = filtersManager.isFilterInEdition(Filter.GenreIs(genre.id, genre.name))
    )
}