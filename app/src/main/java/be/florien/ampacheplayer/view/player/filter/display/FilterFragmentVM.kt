package be.florien.ampacheplayer.view.player.filter.display

import android.databinding.Bindable
import be.florien.ampacheplayer.persistence.local.LocalDataManager
import be.florien.ampacheplayer.persistence.local.model.Filter
import be.florien.ampacheplayer.view.BaseVM
import com.android.databinding.library.baseAdapters.BR
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class FilterFragmentVM
@Inject
constructor(private val localDataManager: LocalDataManager
) : BaseVM() {

    init {
        subscribe(localDataManager.getFilters().observeOn(AndroidSchedulers.mainThread()), onNext = {
            currentFilters.clear()
            currentFilters.addAll(it)
            notifyPropertyChanged(BR.currentFilters)
        })
    }

    @Bindable
    val currentFilters = mutableListOf<Filter<*>>()

    fun clearFilters() {
        subscribe(localDataManager.clearFilters())
    }
}