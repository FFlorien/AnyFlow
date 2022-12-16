package be.florien.anyflow.feature.player.filter.saved

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.FilterGroup
import be.florien.anyflow.databinding.FragmentSavedFilterGroupBinding
import be.florien.anyflow.databinding.ItemFilterGroupBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.player.filter.BaseFilterFragment
import be.florien.anyflow.feature.player.filter.FilterActions

class SavedFilterGroupFragment : BaseFilterFragment() {

    private var singleActionMode: ActionMode? = null
    private lateinit var viewModel: SavedFilterGroupViewModel
    override val filterActions: FilterActions
        get() = viewModel

    private lateinit var binding: FragmentSavedFilterGroupBinding
    private var selectedList = mutableListOf<Int>() //todo selection in vm ?

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater: MenuInflater = mode.menuInflater
            inflater.inflate(R.menu.menu_filter_display, menu)
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
        viewModel = ViewModelProvider(this, requireActivity().viewModelFactory)[SavedFilterGroupViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSavedFilterGroupBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.savedList.layoutManager = GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false)
        binding.savedList.adapter = FilterGroupAdapter()
        viewModel.filterGroups.observe(viewLifecycleOwner) {
            binding.savedList.adapter?.notifyDataSetChanged()
        }
        return binding.root
    }

    inner class FilterGroupAdapter : RecyclerView.Adapter<FilterGroupViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterGroupViewHolder = FilterGroupViewHolder(parent)

        override fun getItemCount(): Int = viewModel.filterGroups.value?.size ?: 0

        override fun onBindViewHolder(holder: FilterGroupViewHolder, position: Int) {
            val group = viewModel.filterGroups.value ?: return
            holder.bind(group[position], selectedList.contains(position)) // todo don't change images, only selection
        }
    }

    inner class FilterGroupViewHolder(
            container: ViewGroup,
            private val itemBinding: ItemFilterGroupBinding = ItemFilterGroupBinding.inflate(LayoutInflater.from(container.context), container, false))
        : RecyclerView.ViewHolder(itemBinding.root) {

        init {
            itemBinding.lifecycleOwner = viewLifecycleOwner
            itemBinding.root.setOnClickListener {
                if (selectedList.isEmpty()) {
                    viewModel.changeForSavedGroup(bindingAdapterPosition)
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
            if (!selectedList.remove(bindingAdapterPosition)) {
                selectedList.add(bindingAdapterPosition)
            }
            binding.savedList.adapter?.notifyItemChanged(bindingAdapterPosition)
        }

        fun bind(filterGroup: FilterGroup, isSelected: Boolean) {
            itemBinding.filterGroup = filterGroup
            itemBinding.isSelected = isSelected
        }
    }
}