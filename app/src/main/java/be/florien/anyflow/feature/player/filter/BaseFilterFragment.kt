package be.florien.anyflow.feature.player.filter

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.observe
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.menu.MenuCoordinator
import be.florien.anyflow.feature.player.PlayerActivity

abstract class BaseFilterFragment : BaseFragment() {

    protected val menuCoordinator = MenuCoordinator()
    protected abstract val baseViewModel: BaseFilterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        baseViewModel.areFiltersInEdition.observe(viewLifecycleOwner) {
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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return menuCoordinator.handleMenuClick(item.itemId)
    }
}