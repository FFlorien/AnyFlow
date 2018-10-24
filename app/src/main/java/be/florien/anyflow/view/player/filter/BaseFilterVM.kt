package be.florien.anyflow.view.player.filter

import android.databinding.Bindable
import be.florien.anyflow.BR
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.view.BaseVM
import javax.inject.Inject

open class BaseFilterVM : BaseVM() {
    @Inject
    lateinit var filtersManager: FiltersManager

    @Bindable
    var areFiltersInEdition: Boolean = true

    fun confirmChanges() {
        subscribe(
                completable = filtersManager.commitChanges(),
                onComplete = {
                    areFiltersInEdition = false
                    notifyPropertyChanged(BR.areFiltersInEdition)
                }
        )
    }

    fun cancelChanges() {
        filtersManager.abandonChanges()
        areFiltersInEdition = false
        notifyPropertyChanged(BR.areFiltersInEdition)
    }
}