package be.florien.anyflow.management.filters

import androidx.lifecycle.LiveData
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.FilterGroup

interface FiltersRepository {
    fun getSavedGroups(): LiveData<List<FilterGroup>>
    fun getCurrentFilters(): LiveData<List<Filter<*>>>
    suspend fun setCurrentFilters(toList: List<Filter<*>>)
    suspend fun saveFilterGroup(toList: List<Filter<*>>, name: String): List<Long>
    suspend fun setSavedGroupAsCurrentFilters(filterGroup: FilterGroup)
}