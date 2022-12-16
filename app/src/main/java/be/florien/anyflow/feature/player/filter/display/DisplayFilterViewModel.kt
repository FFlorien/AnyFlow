package be.florien.anyflow.feature.player.filter.display

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.player.filter.FilterActions
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class DisplayFilterViewModel @Inject constructor(private val filterActions: FilterActions, val dataRepository: DataRepository) :
   BaseViewModel(), FilterActions {

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

    fun getUrlForImage(imageType: String, id: Long): String =
        dataRepository.getArtUrl(imageType, id)

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