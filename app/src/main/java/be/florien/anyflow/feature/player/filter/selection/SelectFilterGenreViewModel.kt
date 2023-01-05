package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
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
    override fun getPagingList(
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> = dataRepository.getGenres(::convert, filters, search)

    override fun isThisTypeOfFilter(filter: Filter<*>)= filter.type == Filter.FilterType.GENRE_IS
    override suspend fun getFoundFilters(
        filters: List<Filter<*>>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getGenresSearchedList(filters, search, ::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(Filter(Filter.FilterType.GENRE_IS, filterValue.id, filterValue.displayName))

    private fun convert(genre: DbGenre): FilterItem {
        val filter = getFilterInParent(Filter(Filter.FilterType.GENRE_IS, genre.id, genre.name))
        return FilterItem(
            genre.id,
            genre.name,
            isSelected = filtersManager.isFilterInEdition(filter)
        )
    }
}