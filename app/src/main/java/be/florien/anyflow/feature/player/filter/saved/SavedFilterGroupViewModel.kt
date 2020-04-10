package be.florien.anyflow.feature.player.filter.saved

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.view.FilterGroup
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.launch
import javax.inject.Inject

class SavedFilterGroupViewModel @Inject constructor(filtersManager: FiltersManager) : BaseFilterViewModel(filtersManager) {
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
}