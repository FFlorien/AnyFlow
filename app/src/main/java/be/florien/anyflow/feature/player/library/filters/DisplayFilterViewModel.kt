package be.florien.anyflow.feature.player.library.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.player.library.LibraryActions
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class DisplayFilterViewModel @Inject constructor(private val libraryActions: LibraryActions, val dataRepository: DataRepository) :
   BaseViewModel(), LibraryActions {

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