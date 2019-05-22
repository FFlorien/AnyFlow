package be.florien.anyflow.view.player.filter

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.databinding.Observable
import be.florien.anyflow.BR
import be.florien.anyflow.view.BaseFragment
import be.florien.anyflow.view.menu.MenuCoordinator
import be.florien.anyflow.view.menu.RollbackMenuHolder
import be.florien.anyflow.view.player.PlayerActivity

abstract class BaseFilterFragment: BaseFragment() {

    protected val menuCoordinator = MenuCoordinator()
    protected abstract val baseVm: BaseFilterVM

    private val cancelMenuHolder = RollbackMenuHolder {
        baseVm.cancelChanges()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        menuCoordinator.addMenuHolder(cancelMenuHolder)
    }

    override fun onResume() {
        super.onResume()
        baseVm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if (propertyId == BR.areFiltersInEdition) {
                    (requireActivity() as PlayerActivity).displaySongList()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menuCoordinator.inflateMenus(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menuCoordinator.prepareMenus(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return menuCoordinator.handleMenuClick(item.itemId)
    }

    override fun onDestroy() {
        super.onDestroy()
        menuCoordinator.removeMenuHolder(cancelMenuHolder)
    }
}