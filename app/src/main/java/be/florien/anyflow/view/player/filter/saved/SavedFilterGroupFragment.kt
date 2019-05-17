package be.florien.anyflow.view.player.filter.saved

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.databinding.Observable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSavedFilterGroupBinding
import be.florien.anyflow.databinding.ItemFilterGroupBinding
import be.florien.anyflow.persistence.local.model.FilterGroup
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterFragment
import be.florien.anyflow.view.player.filter.BaseFilterVM

class SavedFilterGroupFragment : BaseFilterFragment() {

    private var singleActionMode: ActionMode? = null
    private lateinit var vm: SavedFilterGroupVM
    override val baseVm: BaseFilterVM
        get() = vm

    private lateinit var binding: FragmentSavedFilterGroupBinding
    private var selectedList = mutableListOf<Int>() //todo selection in vm ?

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater: MenuInflater = mode.menuInflater
            inflater.inflate(R.menu.menu_filter, menu)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                else -> false
            }
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

        override fun onDestroyActionMode(mode: ActionMode) {
            singleActionMode = null
        }
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
                binding.savedList.adapter?.notifyDataSetChanged()
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
                if (selectedList.isEmpty()) {
                    vm.changeForSavedGroup(adapterPosition)
                } else toggleSelection()
            }
            itemBinding.root.setOnLongClickListener {
                if (singleActionMode != null) {
                    toggleSelection()
                } else {
                    singleActionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
                    toggleSelection()
                }
                true
            }
        }

        private fun toggleSelection() {
            if (!selectedList.remove(adapterPosition)) {
                selectedList.add(adapterPosition)
            }
            binding.savedList.adapter?.notifyItemChanged(adapterPosition)
        }

        fun bind(filterGroup: FilterGroup, coverUrls: List<String>, isSelected: Boolean) {
            itemBinding.filterGroup = filterGroup
            itemBinding.cover1Url = coverUrls[0]
            itemBinding.cover2Url = coverUrls[1]
            itemBinding.cover3Url = coverUrls[2]
            itemBinding.cover4Url = coverUrls[3]
            itemBinding.isSelected = isSelected
        }
    }

    inner class FilterGroupAdapter : RecyclerView.Adapter<FilterGroupViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterGroupViewHolder = FilterGroupViewHolder(parent)

        override fun getItemCount(): Int = vm.filterGroups.size

        override fun onBindViewHolder(holder: FilterGroupViewHolder, position: Int) {
            holder.bind(vm.filterGroups[position], vm.imageForGroups[position], selectedList.contains(position)) // todo don't change images, only selection
        }

    }
}