package be.florien.anyflow.feature.library.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.common.ui.menu.MenuCoordinator
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.library.ui.menu.ConfirmMenuHolder
import kotlinx.coroutines.launch

abstract class BaseFilteringFragment : BaseFragment() {

    protected val menuCoordinator = MenuCoordinator()
    protected abstract val libraryViewModel: LibraryViewModel
    protected abstract val navigator: Navigator

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
        libraryViewModel.areFiltersInEdition.observe(viewLifecycleOwner) {
            if (!it) {
                navigator.navigateToCurrentlyPlaying(requireContext())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {//todo use new menu system
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
    }
}