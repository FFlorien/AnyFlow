package be.florien.anyflow.feature.library.ui.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.feature.auth.domain.net.AuthenticationInterceptor
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.toDisplay
import be.florien.anyflow.feature.library.ui.toItem
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.launch

data class LibraryListState(
    val searchedText: String?,
    val errorMessage: Int?
)

abstract class LibraryListViewModel(
    override val filtersManager: FiltersManager,
    val authenticationInterceptor: AuthenticationInterceptor
) : BaseViewModel(), LibraryViewModel {

    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)
    open val hasSearch = true

    private val isSearching = MutableStateFlow(false)
    private val searchedText = MutableStateFlow("")
    private val errorMessage = MutableStateFlow(-1)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val values: Flow<PagingData<FilterDisplay>> =
        searchedText
            .debounce(300).distinctUntilChanged()
            .flatMapConcat {
                getPagingList(navigationFilter?.clone() as Filter?, it).cachedIn(viewModelScope)
            }
            .combine(filtersManager.filtersInEdition.asFlow()) { paging, edited ->
                paging
                    .filter { !shouldFilterOut(it) }
                    .map {
                        it.toDisplay(hasFilter(it))
                    }
            }
    val state = combine(
        searchedText,
        isSearching,
        errorMessage
    ) { searchedText, isSearching, errorMessage ->
        LibraryListState(
            searchedText = searchedText.takeIf { isSearching },
            errorMessage = errorMessage.takeIf { it > 0 }
        )
    }
    var navigationFilter: Filter? = null

    protected abstract fun getPagingList(
        filter: Filter?,
        search: String?
    ): Flow<PagingData<FilterItem>>

    protected abstract fun isThisTypeOfFilter(filterParam: FilterParam<*>): Boolean
    protected abstract suspend fun getFoundFilters(
        filter: Filter?,
        search: String
    ): List<FilterItem>

    abstract fun getFilter(filterValue: FilterItem): Filter

    fun toggleFilterSelection(filterValue: FilterDisplay) {
        val filter = getFilter(filterValue.toItem())
        if (filtersManager.isFilterInEdition(filter)) {
            filtersManager.removeFilter(filter)
        } else {
            try {
                filtersManager.addFilter(filter)
            } catch (_: FiltersManager.MaxFiltersNumberExceededException) {
                errorMessage.value = R.string.filter_add_error
            }
        }
    }

    fun selectAllInSelection() {
        val search = searchedText.value.takeUnless { it.isEmpty() } ?: return
        viewModelScope.launch(Dispatchers.Main) {
            val changingList = getFoundFilters(navigationFilter?.clone() as Filter?, search)

            run listToAdd@{
                changingList.forEach {
                    val filter = getFilter(it)
                    try {
                        filtersManager.addFilter(filter)
                    } catch (_: FiltersManager.MaxFiltersNumberExceededException) {
                        errorMessage.value = R.string.filter_add_error
                        return@listToAdd
                    }
                }
            }
        }
    }

    fun selectNoneInSelection() {
        val search = searchedText.value.takeUnless { it.isEmpty() } ?: return
        viewModelScope.launch {
            val changingList = filtersManager.filtersInEdition.value?.toList()
            changingList?.forEach {
                if (isThisTypeOfFilter(it.mainParam) && it.mainParam.displayText.contains(search)) {
                    filtersManager.removeFilter(it)
                }
            }
        }
    }

    fun deleteSearch() {
        searchedText.value = ""
    }

    fun hasFilter(filterItem: FilterItem) = filtersManager.isFilterInEdition(getFilter(filterItem))

    fun shouldFilterOut(item: FilterItem): Boolean {
        val filter = navigationFilter?.clone() as Filter?
        return filter != null && (isThisTypeOfFilter(filter.mainParam) && filter.mainParam.argument == item.id)
    }

    protected fun getFilterInParent(filterParam: FilterParam<*>): Filter =
        Filter(*((navigationFilter?.clone() as Filter? ?: emptySet()) + filterParam).toTypedArray())

}