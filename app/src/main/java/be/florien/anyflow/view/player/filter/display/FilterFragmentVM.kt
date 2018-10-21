package be.florien.anyflow.view.player.filter.display

import android.databinding.Bindable
import be.florien.anyflow.player.Filter
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.view.BaseVM
import com.android.databinding.library.baseAdapters.BR
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class FilterFragmentVM
@Inject
constructor(private val filtersManager: FiltersManager
) : BaseVM() {

    init {
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

    fun confirmChanges() {
        subscribe(
                completable = filtersManager.commitChanges(),
                onComplete = {}//todo alert playerActivity?
        )
    }
}