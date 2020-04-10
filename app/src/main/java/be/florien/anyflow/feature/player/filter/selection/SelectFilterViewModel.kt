package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager

abstract class SelectFilterViewModel(filtersManager: FiltersManager) : BaseFilterViewModel(filtersManager) {
    abstract val values: LiveData<PagedList<FilterItem>>
    abstract val itemDisplayType: Int

    protected abstract fun getFilter(filterValue: FilterItem): Filter<*>

    fun changeFilterSelection(filterValue: FilterItem) {
        val filter = getFilter(filterValue)
        if (!filterValue.isSelected) {
            filtersManager.addFilter(filter)
            filterValue.isSelected = true
        } else {
            filtersManager.removeFilter(filter)
            filterValue.isSelected = false
        }
    }

    class FilterItem(val id: Long, val displayName: String, val artUrl: String? = null, var isSelected: Boolean)

    companion object {
        const val ITEM_GRID = 0
        const val ITEM_LIST = 1
    }
}