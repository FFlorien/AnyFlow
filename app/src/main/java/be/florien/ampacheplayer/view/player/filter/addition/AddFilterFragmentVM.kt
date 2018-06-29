package be.florien.ampacheplayer.view.player.filter.addition

import android.databinding.Bindable
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.view.BaseVM

/**
 * Created by FlamentF on 08-Jan-18.
 */
@ActivityScope
abstract class AddFilterFragmentVM<T> : BaseVM() {

    /**
     * Attributes
     */

    protected val values: MutableList<T> = mutableListOf()

    /**
     * Bindables
     */

    @Bindable
    abstract fun getDisplayedValues(): List<FilterItem>

    abstract fun onFilterSelected(filterValue: Long)

    /**
     * Inner classes
     */

    class FilterItem(val id: Long, val displayName: String, val artUrl: String? = null)
}