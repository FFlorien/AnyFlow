package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class SelectFilterViewModel(filtersManager: FiltersManager) :
        BaseFilterViewModel(filtersManager) {
    private var searchJob: Job? = null

    abstract val itemDisplayType: Int
    open val hasSearch = true

    val isSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val searchedText: MutableLiveData<String> = MutableLiveData("")
    var values: LiveData<PagingData<FilterItem>> = MediatorLiveData<PagingData<FilterItem>>().apply {
        addSource(searchedText) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(300)
                getCurrentPagingList(it)
            }
        }

        addSource(filtersManager.filtersInEdition) {
            getCurrentPagingList(searchedText.value)
        }
    }

    protected abstract fun getUnfilteredPagingList(): LiveData<PagingData<FilterItem>>
    protected abstract fun getFilteredPagingList(search: String): LiveData<PagingData<FilterItem>>
    protected abstract suspend fun getFoundFilters(search: String): List<FilterItem>

    protected abstract fun getFilter(filterValue: FilterItem): Filter<*>

    init {
        isSearching.observeForever {
            if (!it) {
                deleteSearch()
            }
        }
    }

    fun changeFilterSelection(filterValue: FilterItem, isSelected: Boolean) {
        val filter = getFilter(filterValue)
        if (isSelected) {
            filtersManager.addFilter(filter)
        } else {
            filtersManager.removeFilter(filter)
        }
    }

    fun selectAllInSelection() {
        viewModelScope.launch {
            val search = searchedText.value ?: return@launch
            val changingList = getFoundFilters(search)
            changingList.forEach {
                val filter = getFilter(it)
                filtersManager.addFilter(filter)
            }
        }
    }

    fun deleteSearch() {
        searchedText.value = ""
    }

    private fun getCurrentPagingList(search: String?): LiveData<PagingData<FilterItem>> {
        val liveData: LiveData<PagingData<FilterItem>> = if (search.isNullOrEmpty()) {
            getUnfilteredPagingList()
        } else {
            getFilteredPagingList(search)
        }
        (values as MediatorLiveData).addSource(liveData) {
            (values as MediatorLiveData).value = it
        }
        return liveData
    }

    fun hasFilter(filterItem: FilterItem): Boolean {
        val filter = getFilter(filterItem)
        return filtersManager.isFilterInEdition(filter)
    }

    class FilterItem(
            val id: Long,
            val displayName: String,
            val artUrl: String? = null,
            val isSelected: Boolean
    )

    companion object {
        const val ITEM_GRID = 0
        const val ITEM_LIST = 1
    }
}