package be.florien.anyflow.feature.player.library.saved

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.FilterGroup
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.player.library.LibraryActions
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.launch
import javax.inject.Inject

class SavedFilterGroupViewModel @Inject constructor(private val libraryActions: LibraryActions) : BaseViewModel(), LibraryActions {
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
        get() = libraryActions.filtersManager
    override val areFiltersInEdition: LiveData<Boolean>
        get() = libraryActions.areFiltersInEdition
    override val currentFilters: LiveData<List<Filter<*>>>
        get() = libraryActions.currentFilters
    override val hasChangeFromCurrentFilters: LiveData<Boolean>
        get() = libraryActions.hasChangeFromCurrentFilters

    override suspend fun confirmChanges() {
        libraryActions.confirmChanges()
    }

    override fun cancelChanges() {
        libraryActions.cancelChanges()
    }

    override suspend fun saveFilterGroup(name: String) {
        libraryActions.saveFilterGroup(name)
    }
}