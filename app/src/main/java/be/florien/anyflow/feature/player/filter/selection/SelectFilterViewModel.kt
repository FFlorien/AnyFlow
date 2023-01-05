package be.florien.anyflow.feature.player.filter.selection

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.player.filter.FilterActions
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class SelectFilterViewModel(private val filterActions: FilterActions) :
    BaseViewModel(), FilterActions {
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
                    getCurrentPagingList(it).cachedIn(viewModelScope)
                }
            }

            addSource(filtersManager.filtersInEdition) {
                getCurrentPagingList(searchedText.value).cachedIn(viewModelScope)
            }
        }
    var parentFilter: Filter<*>? = null

    protected abstract fun getPagingList(
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<FilterItem>>

    protected abstract fun isThisTypeOfFilter(filter: Filter<*>): Boolean
    protected abstract suspend fun getFoundFilters(
        filters: List<Filter<*>>?,
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

    override val filtersManager: FiltersManager
        get() = filterActions.filtersManager
    override val areFiltersInEdition: LiveData<Boolean>
        get() = filterActions.areFiltersInEdition
    override val currentFilters: LiveData<List<Filter<*>>>
        get() = filterActions.currentFilters
    override val hasChangeFromCurrentFilters: LiveData<Boolean>
        get() = filterActions.hasChangeFromCurrentFilters

    override suspend fun confirmChanges() {
        filterActions.confirmChanges()
    }

    override fun cancelChanges() {
        filterActions.cancelChanges()
    }

    override suspend fun saveFilterGroup(name: String) {
        filterActions.saveFilterGroup(name)
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
            val changingList = getFoundFilters(parentFilter?.let { listOf(it) }, search)

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
            getPagingList(parentFilter?.let { listOf(it) }, search)
        if (!liveData.hasActiveObservers()) {
            (values as MediatorLiveData).addSource(liveData) {
                (values as MediatorLiveData).value = it
            }
        }
        return liveData
    }

    fun hasFilter(filterItem: FilterItem) = filtersManager.isFilterInEdition(getFilter(filterItem))

    protected fun getFilterInParent(filter: Filter<*>): Filter<*> =
        parentFilter?.deepCopy()?.apply { addToDeepestChild(filter) } ?: filter

    class FilterItem constructor(
        val id: Long,
        val displayName: String,
        val artUrl: String? = null,
        val isSelected: Boolean
    )
}