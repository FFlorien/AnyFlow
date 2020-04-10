package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterGenreViewModel @Inject constructor(dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    override val values: LiveData<PagedList<FilterItem>> =
            dataRepository.getGenres { genre -> FilterItem(currentId++, genre, isSelected = filtersManager.isFilterInEdition(Filter.GenreIs(genre))) } // todo move this to dataconverter or datarepo

    override val itemDisplayType = ITEM_LIST
    private var currentId = 0L

    override fun getFilter(filterValue: FilterItem) =
            Filter.GenreIs(values.value?.get(filterValue.id.toInt())?.displayName ?: "")
}