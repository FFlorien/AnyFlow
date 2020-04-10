package be.florien.anyflow.feature.player.filter.display

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.launch
import javax.inject.Inject

class DisplayFilterViewModel @Inject constructor(filtersManager: FiltersManager) : BaseFilterViewModel(filtersManager) {

    val currentFilters: LiveData<List<Filter<*>>> = filtersManager.filtersInEdition.map { it.toList() }
    val areFilterGroupExisting: LiveData<Boolean> = filtersManager.filterGroups.map { it.isNotEmpty() }
    val hasChangeFromCurrentFilters: LiveData<Boolean> = filtersManager.hasChange

    fun clearFilters() {
        filtersManager.clearFilters()
    }

    fun deleteFilter(filter: Filter<*>) {
        filtersManager.removeFilter(filter)
    }

    fun resetFilterChanges() {
        filtersManager.abandonChanges()
    }

    fun saveFilterGroup(name: String) {
        viewModelScope.launch {
            filtersManager.saveCurrentFilterGroup(name)
        }
    }
}