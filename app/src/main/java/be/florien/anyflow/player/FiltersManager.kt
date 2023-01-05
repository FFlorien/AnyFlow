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
        if (unCommittedFilters.size >= MAX_FILTER_NUMBER) {
            throw MaxFiltersNumberExceededException()
        }
        if (
            filter.children.isNotEmpty()
            && unCommittedFilters.any { it.equalsIgnoreChildren(filter) }
        ) {
            var child = filter.children.first()
            var parent = unCommittedFilters.first { it.equalsIgnoreChildren(filter) }
            while (
                child.children.isNotEmpty()
                && parent.children.any { it.equalsIgnoreChildren(child) }
            ) {
                parent = parent.children.first { it.equalsIgnoreChildren(child) }
                child = child.children.first()
            }
            parent.children = parent.children.plus(child)
        } else {
            unCommittedFilters.add(filter)
        }
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

    suspend fun saveCurrentFilterGroup(name: String) =
        dataRepository.createFilterGroup(unCommittedFilters.toList(), name)

    suspend fun loadSavedGroup(filterGroup: FilterGroup) =
        dataRepository.setSavedGroupAsCurrentFilters(filterGroup)

    private fun isFiltersTheSame() =
        unCommittedFilters.containsAll(currentFilters) && currentFilters.containsAll(
            unCommittedFilters
        )

    fun abandonChanges() {
        clearFilters()
        unCommittedFilters.addAll(currentFilters)
        areFiltersChanged = false
    }

    fun isFilterInEdition(filter: Filter<*>): Boolean = unCommittedFilters.contains(filter)

    companion object {
        // There is apparently a limit of 1000 characters for queries, which leave us with
        // (1000 / 50 = 20) characters for each filter, not counting the characters before the WHERE
        private const val MAX_FILTER_NUMBER = 50
    }

    class MaxFiltersNumberExceededException : Exception()
}