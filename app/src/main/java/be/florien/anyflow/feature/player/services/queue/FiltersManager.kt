package be.florien.anyflow.feature.player.services.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.FilterGroup
import be.florien.anyflow.injection.ServerScope
import javax.inject.Inject

@ServerScope
class FiltersManager
@Inject constructor(private val queueRepository: QueueRepository) {
    private var currentFilters: List<Filter<*>> = listOf()
    private val unCommittedFilters = mutableSetOf<Filter<*>>()
    private var areFiltersChanged = false
    val filtersInEdition: LiveData<Set<Filter<*>>> = MutableLiveData(setOf())
    val filterGroups = queueRepository.getSavedGroups()

    init {
        queueRepository.getCurrentFilters().observeForever { filters ->
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
            queueRepository.setCurrentFilters(unCommittedFilters.toList())
            areFiltersChanged = false
        }
    }

    suspend fun saveCurrentFilterGroup(name: String) =
        queueRepository.saveFilterGroup(unCommittedFilters.toList(), name)

    suspend fun loadSavedGroup(filterGroup: FilterGroup) =
        queueRepository.setSavedGroupAsCurrentFilters(filterGroup)

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