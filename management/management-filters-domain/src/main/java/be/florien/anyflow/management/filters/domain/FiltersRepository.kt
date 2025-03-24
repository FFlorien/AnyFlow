package be.florien.anyflow.management.filters.domain

import androidx.lifecycle.LiveData
import be.florien.anyflow.management.filters.domain.model.FilterGroup
import be.florien.anyflow.management.filters.domain.model.Filter

interface FiltersRepository {
    fun getSavedGroups(): LiveData<List<FilterGroup>>
    fun getCurrentFilters(): LiveData<List<Filter>>
    suspend fun setCurrentFilters(filters: List<Filter>)
    suspend fun saveFilterGroup(filters: List<Filter>, name: String)
    suspend fun setSavedGroupAsCurrentFilters(filterGroup: FilterGroup)
}