package be.florien.anyflow.feature.library.ui.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.currentFilters
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class LibraryListViewModel(override val filtersManager: FiltersManager) :
    BaseViewModel(), LibraryViewModel {

    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)
    private var searchJob: Job? = null
    open val hasSearch = true

    val isSearching = MutableLiveData(false)
    val searchedText = MutableLiveData("")
    val hasFilterOfThisType: LiveData<Boolean> = currentFilters.map { list ->
        list.any { filter ->
            isThisTypeOfFilter(filter)
        }
    }
    val errorMessage = MutableLiveData(-1)
    var values: LiveData<PagingData<FilterItem>> =
        MediatorLiveData<PagingData<FilterItem>>().apply {
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
    var navigationFilter: Filter<*>? = null

    protected abstract fun getPagingList(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>>

    protected abstract fun isThisTypeOfFilter(filter: Filter<*>): Boolean
    protected abstract suspend fun getFoundFilters(
        filter: Filter<*>?,
        search: String
    ): List<FilterItem>

    abstract fun getFilter(filterValue: FilterItem): Filter<*>

    init {
        isSearching.observeForever {
            if (!it) {
                deleteSearch()
            }
        }
    }

    fun toggleFilterSelection(filterValue: FilterItem) {
        val filter = getFilter(filterValue)
        if (filtersManager.isFilterInEdition(filter)) {
            filtersManager.removeFilter(filter)
        } else {
            try {
                filtersManager.addFilter(filter)
            } catch (exception: FiltersManager.MaxFiltersNumberExceededException) {
                errorMessage.value = R.string.filter_add_error
            }
        }
    }

    fun selectAllInSelection() {
        val search = searchedText.value ?: return
        viewModelScope.launch(Dispatchers.Main) {
            val changingList = getFoundFilters(navigationFilter, search)

            run listToAdd@{
                changingList.forEach {
                    val filter = getFilter(it)
                    try {
                        filtersManager.addFilter(filter)
                    } catch (exception: FiltersManager.MaxFiltersNumberExceededException) {
                        errorMessage.value = R.string.filter_add_error
                        return@listToAdd
                    }
                }
            }
        }
    }

    fun selectNoneInSelection() {
        viewModelScope.launch {
            val search = searchedText.value ?: ""
            val changingList = filtersManager.filtersInEdition.value?.toList()
            changingList?.forEach {
                if (isThisTypeOfFilter(it) && it.displayText.contains(search)) {
                    filtersManager.removeFilter(it)
                }
            }
        }
    }

    fun deleteSearch() {
        searchedText.value = ""
    }

    private fun getCurrentPagingList(search: String?): LiveData<PagingData<FilterItem>> {
        val liveData: LiveData<PagingData<FilterItem>> =
            getPagingList(navigationFilter, search).cachedIn(viewModelScope)
        if (!liveData.hasActiveObservers()) {
            (values as MediatorLiveData).addSource(liveData) {
                (values as MediatorLiveData).value = it
            }
        }
        return liveData
    }

    fun hasFilter(filterItem: FilterItem) = filtersManager.isFilterInEdition(getFilter(filterItem))

    fun shouldFilterOut(item: FilterItem): Boolean {
        var shouldFilterOut = false
        navigationFilter?.traversal {
            shouldFilterOut = shouldFilterOut || (isThisTypeOfFilter(it) && it.argument == item.id)
        }
        return shouldFilterOut
    }

    protected fun getFilterInParent(filter: Filter<*>): Filter<*> =
        navigationFilter?.deepCopy()?.apply { addToDeepestChild(filter) } ?: filter

}