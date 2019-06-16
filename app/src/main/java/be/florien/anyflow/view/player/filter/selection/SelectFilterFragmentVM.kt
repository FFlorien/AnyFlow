package be.florien.anyflow.view.player.filter.selection

import androidx.paging.PagedList
import androidx.databinding.Bindable
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.local.DownloadHelper
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.filter.BaseFilterVM
import javax.inject.Inject

@ActivityScope
abstract class SelectFilterFragmentVM : BaseFilterVM() {

    @Inject
    lateinit var downloadHelper: DownloadHelper

    @Bindable
    var values: PagedList<FilterItem>? = null
    abstract val itemDisplayType: Int

    protected abstract fun getFilter(filterValue: FilterItem): Filter<*>

    abstract fun downloadItem(id: Long)

    fun changeFilterSelection(filterValue: FilterItem) {
        val filter = getFilter(filterValue)
        if (!filterValue.isSelected) {
            filtersManager.addFilter(filter)
            filterValue.isSelected = true
        } else {
            filtersManager.removeFilter(filter)
            filterValue.isSelected = false
        }
    }

    class FilterItem(val id: Long, val displayName: String, val artUrl: String? = null, var isSelected: Boolean)

    companion object {
        const val ITEM_GRID = 0
        const val ITEM_LIST = 1
    }
}