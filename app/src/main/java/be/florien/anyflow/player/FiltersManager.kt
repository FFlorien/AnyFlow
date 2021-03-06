package be.florien.anyflow.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.FilterGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FiltersManager
@Inject constructor(private val dataRepository: DataRepository) {
    private var currentFilters: List<Filter<*>> = listOf()
    private val unCommittedFilters = mutableSetOf<Filter<*>>()
    private var areFiltersChanged = false
    val filtersInEdition: LiveData<Set<Filter<*>>> = MutableLiveData(setOf())
    val filterGroups = dataRepository.getFilterGroups()
    val hasChange: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(filtersInEdition) {
            value = !currentFilters.toTypedArray().contentEquals(it.toTypedArray())
        }
    }

    init {
        dataRepository.getCurrentFilters().observeForever { filters ->
            currentFilters = filters

            if (!areFiltersChanged) {
                unCommittedFilters.clear()
                unCommittedFilters.addAll(filters)
                (filtersInEdition as MutableLiveData).value = unCommittedFilters
            }
        }
    }

    fun addFilter(filter: Filter<*>) {
        unCommittedFilters.add(filter)
        (filtersInEdition as MutableLiveData).value = unCommittedFilters
        areFiltersChanged = true
    }

    fun removeFilter(filter: Filter<*>) {
        unCommittedFilters.remove(filter)
        (filtersInEdition as MutableLiveData).value = unCommittedFilters
        areFiltersChanged = true
    }

    fun clearFilters() {
        unCommittedFilters.clear()
        (filtersInEdition as MutableLiveData).value = unCommittedFilters
        areFiltersChanged = true
    }

    suspend fun commitChanges() {
        if (!isFiltersTheSame()) {
            dataRepository.setCurrentFilters(unCommittedFilters.toList())
            areFiltersChanged = false
        }
    }

    suspend fun saveCurrentFilterGroup(name: String) = dataRepository.createFilterGroup(unCommittedFilters.toList(), name)

    suspend fun loadSavedGroup(filterGroup: FilterGroup) = dataRepository.setSavedGroupAsCurrentFilters(filterGroup)

    private fun isFiltersTheSame() = unCommittedFilters.containsAll(currentFilters) && currentFilters.containsAll(unCommittedFilters)

    fun abandonChanges() {
        clearFilters()
        unCommittedFilters.addAll(currentFilters)
        areFiltersChanged = false
    }

    fun isFilterInEdition(filter: Filter<*>): Boolean = unCommittedFilters.contains(filter)
}