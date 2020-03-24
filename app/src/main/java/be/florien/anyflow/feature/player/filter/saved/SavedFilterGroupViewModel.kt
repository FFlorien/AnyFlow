package be.florien.anyflow.feature.player.filter.saved

import be.florien.anyflow.data.view.FilterGroup
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.feature.MutableValueLiveData
import be.florien.anyflow.feature.ValueLiveData
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.player.FiltersManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class SavedFilterGroupViewModel @Inject constructor(filtersManager: FiltersManager) : BaseFilterViewModel(filtersManager) {
    val filterGroups: ValueLiveData<List<FilterGroup>> = MutableValueLiveData(listOf())
    var imageForGroups: List<List<String>> = listOf()

    init {
        subscribe(filtersManager.filterGroups,
                onNext = {
                    filterGroups.mutable.value = it
                    if (imageForGroups.isEmpty()) {
                        imageForGroups = List(it.size) { List(4) { "" } }
                    }
                },
                onError = {
                    this@SavedFilterGroupViewModel.eLog(it, "Cannot retrieve the filter groups")
                })
        subscribe(filtersManager.artsForFilter,
                onNext = {
                    imageForGroups = it
                })
    }

    fun changeForSavedGroup(savedGroupPosition: Int) {
        subscribe(filtersManager.loadSavedGroup(filterGroups.value[savedGroupPosition]).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()),
                onComplete = {
                    areFiltersInEdition.mutable.value = false
                }
        )
    }
}