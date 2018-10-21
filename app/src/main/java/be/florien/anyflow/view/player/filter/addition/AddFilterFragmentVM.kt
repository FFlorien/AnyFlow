package be.florien.anyflow.view.player.filter.addition

import android.databinding.Bindable
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.view.player.filter.BaseFilterVM

@ActivityScope
abstract class AddFilterFragmentVM<T> : BaseFilterVM() {
    protected val values: MutableList<T> = mutableListOf()
    abstract val itemDisplayType: Int

    @Bindable
    abstract fun getDisplayedValues(): List<FilterItem>

    abstract fun onFilterSelected(filterValue: FilterItem)

    class FilterItem(val id: Long, val displayName: String, val artUrl: String? = null)

    companion object {
        const val ITEM_GRID = 0
        const val ITEM_LIST = 1
    }
}