package be.florien.anyflow.feature.player.filter.saved

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.FilterGroup
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.player.filter.FilterActions
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.launch
import javax.inject.Inject

class SavedFilterGroupViewModel @Inject constructor(private val filterActions: FilterActions) : BaseViewModel(), FilterActions {
    val filterGroups: LiveData<List<FilterGroup>> = filtersManager.filterGroups

    fun changeForSavedGroup(savedGroupPosition: Int) {
        viewModelScope.launch {
            val filterGroup = filterGroups.value?.get(savedGroupPosition)
            if (filterGroup != null) {
                filtersManager.loadSavedGroup(filterGroup)
                areFiltersInEdition.mutable.value = false
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
}