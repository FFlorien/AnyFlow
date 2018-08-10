package be.florien.anyflow.view.player.filter.addition

import android.databinding.Bindable
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.view.BaseVM

@ActivityScope
abstract class AddFilterFragmentVM<T> : BaseVM() {
    protected val values: MutableList<T> = mutableListOf()

    @Bindable
    abstract fun getDisplayedValues(): List<FilterItem>

    abstract fun onFilterSelected(filterValue: FilterItem)

    class FilterItem(val id: Long, val displayName: String, val artUrl: String? = null)
}