package be.florien.anyflow.player

import be.florien.anyflow.persistence.local.LibraryDatabase
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FiltersManager
@Inject constructor(private val libraryDatabase: LibraryDatabase) {
    private var currentFilters: MutableList<Filter<*>> = mutableListOf()
    private val unCommittedFilters = mutableSetOf<Filter<*>>()
    private val filtersInEditionUpdater: BehaviorSubject<Set<Filter<*>>> = BehaviorSubject.create()
    private var areFiltersChanged = false
    val filtersInEdition: Flowable<Set<Filter<*>>> = filtersInEditionUpdater.toFlowable(BackpressureStrategy.LATEST)

    init {
        libraryDatabase
                .getFilters()
                .doOnNext {
                    currentFilters.clear()
                    currentFilters.addAll(it)

                    if (!areFiltersChanged) {
                        unCommittedFilters.clear()
                        unCommittedFilters.addAll(it)
                        filtersInEditionUpdater.onNext(unCommittedFilters)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }

    fun addFilter(filter: Filter<*>) {
        unCommittedFilters.add(filter)
        filtersInEditionUpdater.onNext(unCommittedFilters)
        areFiltersChanged = true
    }

    fun removeFilter(filter: Filter<*>) {
        unCommittedFilters.remove(filter)
        filtersInEditionUpdater.onNext(unCommittedFilters)
        areFiltersChanged = true
    }

    fun clearFilters() {
        unCommittedFilters.clear()
        filtersInEditionUpdater.onNext(unCommittedFilters)
        areFiltersChanged = true
    }

    fun commitChanges(): Completable {
        return libraryDatabase.setFilters(unCommittedFilters.map { it.toDbFilter() }).doOnComplete { areFiltersChanged = false }
    }

    fun abandonChanges() {
        clearFilters()
        unCommittedFilters.addAll(currentFilters)
        areFiltersChanged = false
    }

    fun isFilterInEdition(filter: Filter<*>): Boolean = unCommittedFilters.contains(filter)
}