package be.florien.ampacheplayer.view.player.filter.addition

import android.databinding.Bindable
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.view.BaseVM

@ActivityScope
abstract class AddFilterFragmentVM<T> : BaseVM() {
    protected val values: MutableList<T> = mutableListOf()

    @Bindable
    abstract fun getDisplayedValues(): List<FilterItem>

    abstract fun onFilterSelected(filterValue: FilterItem)

    class FilterItem(val id: Long, val displayName: String, val artUrl: String? = null)
}