package be.florien.anyflow.player

import be.florien.anyflow.persistence.local.LibraryDatabase
import io.reactivex.Completable
import javax.inject.Inject

class FiltersManager
@Inject constructor(private val libraryDatabase: LibraryDatabase) {
    private var currentFilters: MutableList<Filter<*>> = mutableListOf()
    private val unCommittedFilters = mutableListOf<Filter<*>>()
    val filtersInEdition: List<Filter<*>>
        get() = currentFilters.toList()

    fun addFilter(filter: Filter<*>) {
        unCommittedFilters.add(filter)
    }

    fun removeFilter(filter: Filter<*>) {
        unCommittedFilters.remove(filter)
    }

    fun clearFilters() {
        unCommittedFilters.clear()
    }

    fun commit(): Completable {
        return libraryDatabase.setFilters(unCommittedFilters.map { it.toDbFilter() }) //todo make it cleaner and make them listen
    }

    fun abandonChanges() {
        clearFilters()
        unCommittedFilters.addAll(currentFilters)
    }
}