package be.florien.anyflow.management.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.management.filters.domain.FiltersRepository
import be.florien.anyflow.management.filters.domain.model.FilterGroup
import be.florien.anyflow.management.filters.domain.model.Filter
import javax.inject.Inject

@ServerScope
class FiltersManager
@Inject constructor(private val queueRepository: FiltersRepository) {
    private var currentFilters: List<Filter> = listOf()
    private val unCommittedFilters = mutableSetOf<Filter>()
    private var areFiltersChanged = false
    val filtersInEdition: LiveData<Set<Filter>> = MutableLiveData(setOf())
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

    fun addFilter(filter: Filter) {
        if (unCommittedFilters.size >= MAX_FILTER_NUMBER) {
            throw MaxFiltersNumberExceededException()
        }
        unCommittedFilters.add(filter)
        (filtersInEdition as MutableLiveData).value = unCommittedFilters
        areFiltersChanged = true
    }

    fun removeFilter(filter: Filter) {
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

    fun isFilterInEdition(filter: Filter): Boolean {
        return unCommittedFilters.contains(filter)
    }

    companion object {
        // There is apparently a limit of 1000 characters for queries, which leave us with
        // (1000 / 50 = 20) characters for each filter, not counting the characters before the WHERE
        private const val MAX_FILTER_NUMBER = 50
    }

    class MaxFiltersNumberExceededException : Exception()
}