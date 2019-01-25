package be.florien.anyflow.view.player.filter.display

import android.app.Activity
import androidx.databinding.Bindable
import be.florien.anyflow.BR
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterVM
import io.reactivex.android.schedulers.AndroidSchedulers

class DisplayFilterFragmentVM(activity: Activity) : BaseFilterVM() {

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
        subscribe(filtersManager.filtersInEdition.observeOn(AndroidSchedulers.mainThread()), onNext = {
            currentFilters.clear()
            currentFilters.addAll(it)
            notifyPropertyChanged(BR.currentFilters)
        })
    }

    @Bindable
    val currentFilters = mutableListOf<Filter<*>>()

    fun clearFilters() {
        filtersManager.clearFilters()
    }

    fun deleteFilter(filter: Filter<*>) {
        filtersManager.removeFilter(filter)
    }

    fun resetFilterChanges() {
        filtersManager.abandonChanges()
    }
}