package be.florien.anyflow.feature.library.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter

interface LibraryViewModel { //todo rename and probably move because it's library AND filter related
    val filtersManager: FiltersManager
    val areFiltersInEdition: LiveData<Boolean>
    val navigator: Navigator
}

val LibraryViewModel.currentFilters: LiveData<Set<Filter>>
    get() = filtersManager.filtersInEdition

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
