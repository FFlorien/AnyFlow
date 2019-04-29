package be.florien.anyflow.view.player.filter

import androidx.databinding.Observable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import be.florien.anyflow.BR
import be.florien.anyflow.view.BaseFragment
import be.florien.anyflow.view.menu.CancelMenuHolder
import be.florien.anyflow.view.menu.ConfirmMenuHolder
import be.florien.anyflow.view.menu.MenuCoordinator
import be.florien.anyflow.view.player.PlayerActivity

abstract class BaseFilterFragment: BaseFragment() {

    protected val menuCoordinator = MenuCoordinator()
    protected abstract val baseVm: BaseFilterVM

    private var confirmMenuHolder: ConfirmMenuHolder = ConfirmMenuHolder {
        baseVm.confirmChanges()
    }

    private val cancelMenuHolder = CancelMenuHolder {
        baseVm.cancelChanges()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        confirmMenuHolder
        menuCoordinator.addMenuHolder(confirmMenuHolder)
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
        menuCoordinator.removeMenuHolder(confirmMenuHolder)
        menuCoordinator.removeMenuHolder(cancelMenuHolder)
    }
}