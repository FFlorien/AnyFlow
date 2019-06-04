package be.florien.anyflow.view.player.filter.saved

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.databinding.Observable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSavedFilterGroupBinding
import be.florien.anyflow.databinding.ItemFilterGroupBinding
import be.florien.anyflow.persistence.local.model.FilterGroup
import be.florien.anyflow.view.menu.DeleteMenuHolder
import be.florien.anyflow.view.menu.EditMenuHolder
import be.florien.anyflow.view.menu.MenuCoordinator
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterFragment
import be.florien.anyflow.view.player.filter.BaseFilterVM

class SavedFilterGroupFragment : BaseFilterFragment(), ActionMode.Callback {

    private var actionMode: ActionMode? = null
    private lateinit var vm: SavedFilterGroupVM
    private val contextualMenuCoordinator = MenuCoordinator()
    private val deleteMenuHolder = DeleteMenuHolder {
        vm.deleteSelection()
        vm.resetSelection()
    }
    private val editMenuHolder = EditMenuHolder {
        if (vm.selectionList.size == 1) {
            val (_, name) = vm.selectionList[0]
            val editText = EditText(requireActivity())
            editText.setText(name)
            editText.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
            AlertDialog.Builder(requireActivity())
                    .setView(editText)
                    .setTitle(R.string.filter_group_name)
                    .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                        vm.changeSelectedGroupName(editText.text.toString())
                    }
                    .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
                        dialog.cancel()
                    }
                    .show()
        }
    }
    override val baseVm: BaseFilterVM
        get() = vm

    private lateinit var binding: FragmentSavedFilterGroupBinding

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val inflater: MenuInflater = mode.menuInflater
        contextualMenuCoordinator.addMenuHolder(deleteMenuHolder)
        contextualMenuCoordinator.addMenuHolder(editMenuHolder)
        contextualMenuCoordinator.inflateMenus(menu, inflater)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return contextualMenuCoordinator.handleMenuClick(item.itemId)
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        contextualMenuCoordinator.prepareMenus(menu)
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        contextualMenuCoordinator.removeAllMenuHolder()
        vm.resetSelection()
    }

    override fun getTitle(): String = getString(R.string.filter_title_saved)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        vm = SavedFilterGroupVM(requireActivity() as PlayerActivity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSavedFilterGroupBinding.inflate(inflater, container, false)
        binding.savedList.layoutManager = GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false)
        binding.savedList.adapter = FilterGroupAdapter()
        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                when (propertyId) {
                    BR.filterGroups,
                    BR.imageForGroups -> binding.savedList.adapter?.notifyDataSetChanged()
                    BR.selectionList -> {
                        if (vm.selectionList.isNotEmpty() && actionMode == null) {
                            actionMode = (requireActivity() as PlayerActivity).toolbar.startActionMode(this@SavedFilterGroupFragment)
                        } else if (vm.selectionList.isNotEmpty()) {
                            editMenuHolder.isVisible = vm.selectionList.size == 1
                        } else if (vm.selectionList.isEmpty()) {
                            binding.savedList.adapter?.notifyDataSetChanged()
                            actionMode?.finish()
                            actionMode = null
                        }
                    }
                }
            }

        })
        return binding.root
    }

    inner class FilterGroupViewHolder(
            container: ViewGroup,
            private val itemBinding: ItemFilterGroupBinding = ItemFilterGroupBinding.inflate(LayoutInflater.from(container.context), container, false))
        : RecyclerView.ViewHolder(itemBinding.root) {

        init {
            itemBinding.root.setOnClickListener {
                if (vm.selectionList.isEmpty()) {
                    vm.changeForSavedGroup(adapterPosition)
                } else toggleSelection()
            }
            itemBinding.root.setOnLongClickListener {
                toggleSelection()
                true
            }
        }

        private fun toggleSelection() {
            vm.toggleGroupSelection(adapterPosition)
            binding.savedList.adapter?.notifyItemChanged(adapterPosition)
        }

        fun bind(filterGroup: FilterGroup, coverUrls: List<String>, isSelected: Boolean) {
            if (itemBinding.filterGroup != filterGroup) {
                itemBinding.filterGroup = filterGroup
            }
            if (itemBinding.cover1Url != coverUrls[0] || itemBinding.cover2Url != coverUrls[1] || itemBinding.cover3Url != coverUrls[2] || itemBinding.cover4Url != coverUrls[3]) {
                itemBinding.cover1Url = coverUrls[0]
                itemBinding.cover2Url = coverUrls[1]
                itemBinding.cover3Url = coverUrls[2]
                itemBinding.cover4Url = coverUrls[3]
            }
            itemBinding.isSelected = isSelected
        }
    }

    inner class FilterGroupAdapter : RecyclerView.Adapter<FilterGroupViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterGroupViewHolder = FilterGroupViewHolder(parent)

        override fun getItemCount(): Int = vm.filterGroups.size

        override fun onBindViewHolder(holder: FilterGroupViewHolder, position: Int) {
            holder.bind(vm.filterGroups[position], vm.imageForGroups[position], vm.isGroupSelected(position))
        }

    }
}