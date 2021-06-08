package be.florien.anyflow.feature.player.filter

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import be.florien.anyflow.R
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.menu.ConfirmMenuHolder
import be.florien.anyflow.feature.menu.MenuCoordinator
import be.florien.anyflow.feature.menu.SaveFilterGroupMenuHolder
import be.florien.anyflow.feature.player.PlayerActivity

abstract class BaseFilterFragment : BaseFragment() {

    protected val menuCoordinator = MenuCoordinator()
    protected abstract val baseViewModel: BaseFilterViewModel

    private val confirmMenuHolder = ConfirmMenuHolder {
        baseViewModel.confirmChanges()
    }
    protected val saveMenuHolder = SaveFilterGroupMenuHolder {
        val editText = EditText(requireActivity())
        editText.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
        AlertDialog.Builder(requireActivity())
            .setView(editText)
            .setTitle(R.string.filter_group_name)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                baseViewModel.saveFilterGroup(editText.text.toString())
            }
            .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuCoordinator.addMenuHolder(confirmMenuHolder)
        menuCoordinator.addMenuHolder(saveMenuHolder)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        confirmMenuHolder.isVisible = baseViewModel.hasChangeFromCurrentFilters.value == true
        baseViewModel.hasChangeFromCurrentFilters.observe(viewLifecycleOwner) {
            confirmMenuHolder.isVisible = it == true
        }
        saveMenuHolder.isVisible = false
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

    override fun onDestroy() {
        super.onDestroy()
        menuCoordinator.removeMenuHolder(confirmMenuHolder)
        menuCoordinator.removeMenuHolder(saveMenuHolder)
    }
}