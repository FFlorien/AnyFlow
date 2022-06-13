package be.florien.anyflow.feature.player.filter.display

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class DisplayFilterViewModel @Inject constructor(filtersManager: FiltersManager) :
    BaseFilterViewModel(filtersManager) {

    val areFilterGroupExisting: LiveData<Boolean> =
        filtersManager.filterGroups.map { it.isNotEmpty() }

    fun clearFilters() {
        filtersManager.clearFilters()
    }

    fun deleteFilter(filter: Filter<*>) {
        filtersManager.removeFilter(filter)
    }

    fun resetFilterChanges() {
        filtersManager.abandonChanges()
    }
}