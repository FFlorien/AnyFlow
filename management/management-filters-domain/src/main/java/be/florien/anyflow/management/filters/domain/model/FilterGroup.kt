package be.florien.anyflow.management.filters.domain.model

import java.util.Calendar

sealed class FilterGroup(
    open val id: Long,
    open val filters: List<Filter>
) {
    data class CurrentFilterGroup(override val id: Long, override val filters: List<Filter>) : FilterGroup(id, filters)
    data class HistoryFilterGroup(override val id: Long, override val filters: List<Filter>, val dateAdded: Calendar) : FilterGroup(id, filters)
    data class SavedFilterGroup(override val id: Long, override val filters: List<Filter>, val dateAdded: Calendar, val name: String) :
        FilterGroup(id, filters)
}