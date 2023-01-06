package be.florien.anyflow.feature.player.library.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import be.florien.anyflow.databinding.ItemSelectFilterListBinding
import be.florien.anyflow.feature.BaseSelectableAdapter
import be.florien.anyflow.feature.player.details.DetailViewHolder
import be.florien.anyflow.feature.player.details.DetailViewHolderListener
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView


class FilterListAdapter(
    override val isSelected: (LibraryListViewModel.FilterItem) -> Boolean,
    override val setSelected: (LibraryListViewModel.FilterItem) -> Unit,
    private val detailListener: DetailViewHolderListener<LibraryListViewModel.FilterItem>
) : PagingDataAdapter<LibraryListViewModel.FilterItem, FilterViewHolder>(object :
    DiffUtil.ItemCallback<LibraryListViewModel.FilterItem>() {
    override fun areItemsTheSame(
        oldItem: LibraryListViewModel.FilterItem,
        newItem: LibraryListViewModel.FilterItem
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: LibraryListViewModel.FilterItem,
        newItem: LibraryListViewModel.FilterItem
    ): Boolean =
        oldItem.artConfig == newItem.artConfig && oldItem.displayName == newItem.displayName && oldItem.isSelected == newItem.isSelected
}), FastScrollRecyclerView.SectionedAdapter,
    BaseSelectableAdapter<LibraryListViewModel.FilterItem> {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder =
        FilterViewHolder(parent, detailListener, setSelected)

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = getItem(position) ?: return
        val isSelected = isSelected(filter)
        holder.bind(filter, isSelected)
    }

    override fun getSectionName(position: Int): String =
        snapshot()[position]?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
}

class FilterViewHolder(
    parent: ViewGroup,
    detailListener: DetailViewHolderListener<LibraryListViewModel.FilterItem>,
    override val onSelectChange: (LibraryListViewModel.FilterItem) -> Unit,
    private val binding: ItemSelectFilterListBinding = ItemSelectFilterListBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    )
) : DetailViewHolder<LibraryListViewModel.FilterItem>(
    detailListener,
    binding.root
),
    BaseSelectableAdapter.BaseSelectableViewHolder<LibraryListViewModel.FilterItem, LibraryListViewModel.FilterItem> {

    override val itemInfoView: View
        get() = binding.info
    override val infoIconView: View
        get() = binding.infoView
    override val item: LibraryListViewModel.FilterItem?
        get() = binding.item

    init {
        binding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        setClickListener()
    }

    override fun bind(item: LibraryListViewModel.FilterItem, isSelected: Boolean) {
        binding.item = item
        setSelection(isSelected)
        itemInfoView.setOnClickListener {
            onSelectChange(item)
        }
    }

    override fun setSelection(isSelected: Boolean) {
        binding.selected = isSelected
    }

    override fun getCurrentId(): LibraryListViewModel.FilterItem? = binding.item
}