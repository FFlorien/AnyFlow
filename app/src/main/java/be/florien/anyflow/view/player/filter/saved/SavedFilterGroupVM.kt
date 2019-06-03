package be.florien.anyflow.view.player.filter.saved

import androidx.databinding.Bindable
import be.florien.anyflow.BR
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.persistence.local.model.FilterGroup
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterVM
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SavedFilterGroupVM(activity: PlayerActivity) : BaseFilterVM() {

    @Bindable
    var filterGroups: List<FilterGroup> = listOf()

    @Bindable
    var imageForGroups: List<List<String>> = listOf()

    private val _selectionList = mutableListOf<FilterGroup>()

    @Bindable
    val selectionList: List<FilterGroup> = _selectionList

    init {
        activity.activityComponent.inject(this)
        subscribe(filtersManager.filterGroups,
                onNext = {
                    filterGroups = it
                    if (imageForGroups.isEmpty()) {
                        imageForGroups = List(filterGroups.size) { List(4) { "" } }
                    }
                    notifyPropertyChanged(BR.filterGroups)
                },
                onError = {
                    this@SavedFilterGroupVM.eLog(it, "Cannot retrieve the filter groups")
                })
        subscribe(filtersManager.artsForFilter,
                onNext = {
                    imageForGroups = it
                    notifyPropertyChanged(BR.imageForGroups)
                })
    }

    fun changeForSavedGroup(savedGroupPosition: Int) {
        subscribe(filtersManager.loadSavedGroup(filterGroups[savedGroupPosition]).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()),
                onComplete = {
                    areFiltersInEdition = false
                    notifyPropertyChanged(BR.areFiltersInEdition)
                }
        )
    }

    fun toggleGroupSelection(position: Int) {
        if (!_selectionList.remove(filterGroups[position])) {
            _selectionList.add(filterGroups[position])
        }
        notifyPropertyChanged(BR.selectionList)
    }

    fun isGroupSelected(position: Int) = _selectionList.contains(filterGroups[position])

    fun resetSelection() {
        _selectionList.clear()
        notifyPropertyChanged(BR.selectionList)
    }

    fun deleteSelection() {
        subscribe(filtersManager.deleteFilterGroups(_selectionList.toList()))
    }

    fun changeSelectedGroupName(newName: String) {
        subscribe(filtersManager.changeGroupName(_selectionList[0], newName),
                onComplete = {
                    resetSelection()
                })
    }
}