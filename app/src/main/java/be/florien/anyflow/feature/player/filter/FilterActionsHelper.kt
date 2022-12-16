package be.florien.anyflow.feature.player.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager

interface FilterActions {
    val filtersManager: FiltersManager
    val areFiltersInEdition: LiveData<Boolean>
    val currentFilters: LiveData<List<Filter<*>>>
    val hasChangeFromCurrentFilters: LiveData<Boolean>

    suspend fun confirmChanges()
    fun cancelChanges()
    suspend fun saveFilterGroup(name: String)
}

class FilterActionsHelper(override val filtersManager: FiltersManager) : FilterActions {

    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)
    override val currentFilters: LiveData<List<Filter<*>>> =
        filtersManager.filtersInEdition.map { it.toList() }
    override val hasChangeFromCurrentFilters: LiveData<Boolean> = filtersManager.hasChange

    override suspend fun confirmChanges() {
        filtersManager.commitChanges()
        (areFiltersInEdition as MutableLiveData).value = false
    }

    override fun cancelChanges() {
        filtersManager.abandonChanges()
        (areFiltersInEdition as MutableLiveData).value = false
    }

    override suspend fun saveFilterGroup(name: String) {
        filtersManager.saveCurrentFilterGroup(name)
    }
}