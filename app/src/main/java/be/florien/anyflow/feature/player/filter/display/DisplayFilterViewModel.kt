package be.florien.anyflow.feature.player.filter.display

import be.florien.anyflow.feature.MutableValueLiveData
import be.florien.anyflow.feature.ValueLiveData
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class DisplayFilterViewModel @Inject constructor(filtersManager: FiltersManager) : BaseFilterViewModel(filtersManager) {

    init {
        subscribe(filtersManager.filtersInEdition.observeOn(AndroidSchedulers.mainThread()), onNext = {
            currentFilters.mutable.value = it.toMutableList()
        })
        subscribe(filtersManager.filterGroups.map { it.isNotEmpty() }, onNext = {
            areFilterGroupExisting.mutable.value = it
        })
        subscribe(filtersManager.hasChange.observeOn(AndroidSchedulers.mainThread()), onNext = {
            hasChangeFromCurrentFilters.mutable.value = it
        })
    }

    val currentFilters: ValueLiveData<List<Filter<*>>> = MutableValueLiveData(mutableListOf())
    val areFilterGroupExisting: ValueLiveData<Boolean> = MutableValueLiveData(false)
    val hasChangeFromCurrentFilters: ValueLiveData<Boolean> = MutableValueLiveData(false)

    fun clearFilters() {
        filtersManager.clearFilters()
    }

    fun deleteFilter(filter: Filter<*>) {
        filtersManager.removeFilter(filter)
    }

    fun resetFilterChanges() {
        filtersManager.abandonChanges()
    }

    fun saveFilterGroup(name: String) {
        filtersManager.saveCurrentFilterGroup(name).subscribe()
    }
}