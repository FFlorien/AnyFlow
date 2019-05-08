package be.florien.anyflow.view.player.filter.saved

import androidx.databinding.Bindable
import be.florien.anyflow.BR
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.persistence.local.model.FilterGroup
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterVM

class SavedFilterGroupVM(activity: PlayerActivity) : BaseFilterVM() {

    @Bindable
    var filterGroups: List<FilterGroup> = listOf()

    @Bindable
    var imageForGroups: List<List<String>> = listOf()

    init {
        activity.activityComponent.inject(this)
        subscribe(filtersManager.filterGroups,
                onNext = {
                    filterGroups = it
                    if (imageForGroups.isEmpty()) {
                        imageForGroups = List(filterGroups.size) { List(4) {""} }
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
}