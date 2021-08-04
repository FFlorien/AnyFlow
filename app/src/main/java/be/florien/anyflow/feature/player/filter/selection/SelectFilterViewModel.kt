package be.florien.anyflow.feature.player.filter.selection

import android.text.TextWatcher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.player.FiltersManager

abstract class SelectFilterViewModel(filtersManager: FiltersManager) :
    BaseFilterViewModel(filtersManager) {
    abstract val values: LiveData<PagingData<FilterItem>>
    abstract val itemDisplayType: Int
    abstract val searchTextWatcher: TextWatcher
    val isSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val searchedText: MutableLiveData<String> = MutableLiveData("")

    protected abstract fun getFilter(filterValue: FilterItem): Filter<*>

    init {
        isSearching.observeForever {
            if (!it) {
                deleteSearch()
            }
        }
    }

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

    fun deleteSearch() {
        searchedText.value = ""
    }

    class FilterItem(
        val id: Long,
        val displayName: String,
        val artUrl: String? = null,
        var isSelected: Boolean
    )

    companion object {
        const val ITEM_GRID = 0
        const val ITEM_LIST = 1
    }
}