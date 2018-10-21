package be.florien.anyflow.player

import be.florien.anyflow.persistence.local.LibraryDatabase
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
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


    private var subscribe: Disposable?

    init {
        subscribe = libraryDatabase
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
    }

    fun removeFilter(filter: Filter<*>) {
        unCommittedFilters.remove(filter)
        filtersInEditionUpdater.onNext(unCommittedFilters)
    }

    fun clearFilters() {
        unCommittedFilters.clear()
        filtersInEditionUpdater.onNext(unCommittedFilters)
    }

    fun commitChanges(): Completable {
        return libraryDatabase.setFilters(unCommittedFilters.map { it.toDbFilter() }) //todo make it cleaner and make them listen
    }

    fun abandonChanges() {
        clearFilters()
        unCommittedFilters.addAll(currentFilters)
    }

    fun destroy() {//todo
        subscribe?.dispose()
    }
}