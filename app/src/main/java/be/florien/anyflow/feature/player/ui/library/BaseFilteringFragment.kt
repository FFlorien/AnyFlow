package be.florien.anyflow.feature.player.ui.library

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.menu.MenuCoordinator
import be.florien.anyflow.feature.menu.implementation.ConfirmMenuHolder
import be.florien.anyflow.feature.player.ui.PlayerActivity
import kotlinx.coroutines.launch

abstract class BaseFilteringFragment : BaseFragment() {

    protected val menuCoordinator = MenuCoordinator()
    protected abstract val libraryViewModel: LibraryViewModel

    private val confirmMenuHolder = ConfirmMenuHolder {
        lifecycleScope.launch {
            libraryViewModel.confirmChanges()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuCoordinator.addMenuHolder(confirmMenuHolder)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.hasChangeFromCurrentFilters.observe(viewLifecycleOwner) {
            confirmMenuHolder.isEnabled = it == true
        }
        libraryViewModel.areFiltersInEdition.observe(viewLifecycleOwner) {
            if (!it) {
                (requireActivity() as PlayerActivity).displaySongList()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menuCoordinator.inflateMenus(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menuCoordinator.prepareMenus(menu)
        confirmMenuHolder.isEnabled = libraryViewModel.hasChangeFromCurrentFilters.value == true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return menuCoordinator.handleMenuClick(item.itemId)
    }

    override fun onDestroy() {
        super.onDestroy()
        menuCoordinator.removeMenuHolder(confirmMenuHolder)
    }
}