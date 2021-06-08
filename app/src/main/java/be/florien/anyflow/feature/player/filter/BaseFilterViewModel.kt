package be.florien.anyflow.feature.player.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.launch

open class BaseFilterViewModel(protected val filtersManager: FiltersManager) : BaseViewModel() {

    val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)
    val currentFilters: LiveData<List<Filter<*>>> = filtersManager.filtersInEdition.map { it.toList() }
    val hasChangeFromCurrentFilters: LiveData<Boolean> = filtersManager.hasChange

    fun confirmChanges() {
        viewModelScope.launch {
            filtersManager.commitChanges()
            areFiltersInEdition.mutable.value = false
        }
    }

    fun cancelChanges() {
        filtersManager.abandonChanges()
        areFiltersInEdition.mutable.value = false
    }

    fun saveFilterGroup(name: String) {
        viewModelScope.launch {
            filtersManager.saveCurrentFilterGroup(name)
        }
    }
}