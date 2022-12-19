package be.florien.anyflow.feature.player.filter.selection

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
    override val isSelected: (SelectFilterViewModel.FilterItem) -> Boolean,
    override val setSelected: (SelectFilterViewModel.FilterItem) -> Unit,
    private val detailListener: DetailViewHolderListener<SelectFilterViewModel.FilterItem>
) : PagingDataAdapter<SelectFilterViewModel.FilterItem, FilterViewHolder>(object :
    DiffUtil.ItemCallback<SelectFilterViewModel.FilterItem>() {
    override fun areItemsTheSame(
        oldItem: SelectFilterViewModel.FilterItem,
        newItem: SelectFilterViewModel.FilterItem
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: SelectFilterViewModel.FilterItem,
        newItem: SelectFilterViewModel.FilterItem
    ): Boolean =
        oldItem.artUrl == newItem.artUrl && oldItem.displayName == newItem.displayName && oldItem.isSelected == newItem.isSelected
}), FastScrollRecyclerView.SectionedAdapter,
    BaseSelectableAdapter<SelectFilterViewModel.FilterItem> {
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
    detailListener: DetailViewHolderListener<SelectFilterViewModel.FilterItem>,
    override val onSelectChange: (SelectFilterViewModel.FilterItem) -> Unit,
    private val binding: ItemSelectFilterListBinding = ItemSelectFilterListBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    )
) : DetailViewHolder<SelectFilterViewModel.FilterItem>(
    detailListener,
    binding.root
),
    BaseSelectableAdapter.BaseSelectableViewHolder<SelectFilterViewModel.FilterItem, SelectFilterViewModel.FilterItem> {

    override val itemInfoView: View
        get() = binding.info
    override val infoIconView: View
        get() = binding.infoView
    override val item: SelectFilterViewModel.FilterItem?
        get() = binding.item

    init {
        binding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        setClickListener()
    }

    override fun bind(item: SelectFilterViewModel.FilterItem, isSelected: Boolean) {
        binding.item = item
        setSelection(isSelected)
        itemInfoView.setOnClickListener {
            onSelectChange(item)
        }
    }

    override fun setSelection(isSelected: Boolean) {
        binding.selected = isSelected
    }

    override fun getCurrentId(): SelectFilterViewModel.FilterItem? = binding.item
}