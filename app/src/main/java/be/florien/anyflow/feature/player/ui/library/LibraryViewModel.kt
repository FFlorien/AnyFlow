package be.florien.anyflow.feature.player.ui.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.services.queue.FiltersManager

interface LibraryViewModel {
    val filtersManager: FiltersManager
    val areFiltersInEdition: LiveData<Boolean>
}

val LibraryViewModel.currentFilters: LiveData<Set<Filter<*>>>
    get() = filtersManager.filtersInEdition
val LibraryViewModel.hasChangeFromCurrentFilters: LiveData<Boolean>
    get() = filtersManager.hasChange

suspend fun LibraryViewModel.confirmChanges() {
    filtersManager.commitChanges()
    (areFiltersInEdition as MutableLiveData).value = false
}

fun LibraryViewModel.cancelChanges() {
    filtersManager.abandonChanges()
    (areFiltersInEdition as MutableLiveData).value = false
}

suspend fun LibraryViewModel.saveFilterGroup(name: String) {
    filtersManager.saveCurrentFilterGroup(name)
}
