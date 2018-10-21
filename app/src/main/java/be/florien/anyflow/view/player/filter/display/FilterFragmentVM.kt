package be.florien.anyflow.view.player.filter.display

import android.app.Activity
import android.databinding.Bindable
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterVM
import com.android.databinding.library.baseAdapters.BR
import io.reactivex.android.schedulers.AndroidSchedulers

class FilterFragmentVM(activity: Activity) : BaseFilterVM() {

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
}