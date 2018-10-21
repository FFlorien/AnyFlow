package be.florien.anyflow.view.player.filter

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import be.florien.anyflow.view.menu.ConfirmMenuHolder
import be.florien.anyflow.view.menu.MenuCoordinator

abstract class BaseFilterFragment: Fragment() {

    private val menuCoordinator = MenuCoordinator()
    protected abstract val baseVm: BaseFilterVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        menuCoordinator.addMenuHolder(ConfirmMenuHolder {
            baseVm.confirmChanges()
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
}