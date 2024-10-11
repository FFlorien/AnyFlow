package be.florien.anyflow.feature.library.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter

interface LibraryViewModel { //todo rename and probably move because it's library AND filter related
    val filtersManager: FiltersManager
    val areFiltersInEdition: LiveData<Boolean>
    val navigator: be.florien.anyflow.common.navigation.Navigator
}

val LibraryViewModel.currentFilters: LiveData<Set<Filter<*>>>
    get() = filtersManager.filtersInEdition

val LibraryViewModel.currentFiltersForDisplay: LiveData<List<Filter<*>>> //todo it's only used by filters VM
    get() = currentFilters.map { filters ->
        val result = mutableListOf<Filter<*>>()
        filters.forEach { base ->
            val baseCopy = base.copy(children = emptyList())

            fun traverse(currentFilter: Filter<*>) {
                val currentFilterCopy = currentFilter.copy(children = emptyList())
                baseCopy.addToDeepestChild(currentFilterCopy)
                if (currentFilter.children.isNotEmpty()) {
                    currentFilter.children.forEach { child ->
                        traverse(child)
                    }
                } else {
                    result.add(baseCopy.deepCopy())
                    baseCopy.traversal {
                        if (it.children.contains(currentFilterCopy)) {
                            it.children = emptyList()
                        }
                    }
                }
            }
            if (base.children.isEmpty()) {
                result.add(baseCopy)
            } else {
                base.children.forEach { currentFilter ->
                    traverse(currentFilter)
                }
            }
        }
        result
    }

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
