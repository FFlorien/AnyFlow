package be.florien.anyflow.player

import be.florien.anyflow.extension.eLog
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.model.FilterGroup
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
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
    val filterGroups = libraryDatabase.getFilterGroups()
    val artsForFilter = libraryDatabase.getAlbumArtsForFilterGroup()
    val hasChange: Flowable<Boolean> =
            filtersInEdition.map {it.toList() }
                    .withLatestFrom(
                            libraryDatabase.getCurrentFilters(),
                            BiFunction { currentFilters: List<Filter<*>>, filterInEdition: List<Filter<*>> ->
                                !currentFilters.toTypedArray().contentEquals(filterInEdition.toTypedArray())
                            })
                    .doOnError { this@FiltersManager.eLog(it, "Error while querying hasChange") }

    init {
        libraryDatabase
                .getCurrentFilters()
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
        return if (isFiltersTheSame()) {
            Completable.complete()
        } else {
            libraryDatabase.setCurrentFilters(unCommittedFilters.toList()).doOnComplete { areFiltersChanged = false }
        }
    }

    fun saveCurrentFilterGroup(name: String): Completable = libraryDatabase.createFilterGroup(unCommittedFilters.toList(), name)

    fun loadSavedGroup(filterGroup: FilterGroup): Completable = libraryDatabase.setCurrentFiltersOfSavedGroup(filterGroup)

    private fun isFiltersTheSame() = unCommittedFilters.containsAll(currentFilters) && currentFilters.containsAll(unCommittedFilters)

    fun abandonChanges() {
        clearFilters()
        unCommittedFilters.addAll(currentFilters)
        areFiltersChanged = false
    }

    fun isFilterInEdition(filter: Filter<*>): Boolean = unCommittedFilters.contains(filter)
}