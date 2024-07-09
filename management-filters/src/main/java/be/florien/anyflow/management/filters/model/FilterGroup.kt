package be.florien.anyflow.management.filters.model

import java.util.Calendar

sealed class FilterGroup(
    open val id: Long
) {
    data class CurrentFilterGroup(override val id: Long) : FilterGroup(id)
    data class HistoryFilterGroup(override val id: Long, val dateAdded: Calendar) : FilterGroup(id)
    data class SavedFilterGroup(override val id: Long, val dateAdded: Calendar, val name: String) :
        FilterGroup(id)
}