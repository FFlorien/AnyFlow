package be.florien.anyflow.feature.library.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.list.BaseSelectableAdapter
import be.florien.anyflow.common.ui.list.DetailViewHolder
import be.florien.anyflow.common.ui.list.DetailViewHolderListener
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.ui.databinding.ItemSelectFilterListBinding
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

val diffCallback = object : DiffUtil.ItemCallback<FilterItem>() {
    override fun areItemsTheSame(
        oldItem: FilterItem,
        newItem: FilterItem
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: FilterItem,
        newItem: FilterItem
    ): Boolean =
        oldItem.artUrl == newItem.artUrl && oldItem.displayName == newItem.displayName && oldItem.isSelected == newItem.isSelected
}

class FilterListAdapter(
    override val isSelected: (FilterItem) -> Boolean,
    override val setSelected: (FilterItem) -> Unit,
    private val detailListener: DetailViewHolderListener<FilterItem>
) : PagingDataAdapter<FilterItem, FilterViewHolder>(diffCallback),
    FastScrollRecyclerView.SectionedAdapter,
    BaseSelectableAdapter<FilterItem> {
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
    detailListener: DetailViewHolderListener<FilterItem>,
    override val onSelectChange: (FilterItem) -> Unit,
    private val binding: ItemSelectFilterListBinding =
        ItemSelectFilterListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
) : DetailViewHolder<FilterItem>(detailListener, binding.root),
    BaseSelectableAdapter.BaseSelectableViewHolder<FilterItem, FilterItem> {

    override val itemInfoView: View
        get() = binding.info
    override val infoIconView: View
        get() = binding.infoView
    override val item: FilterItem?
        get() = binding.item

    init {
        binding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        setClickListener()
    }

    override fun bind(item: FilterItem, isSelected: Boolean) {
        binding.item = item
        binding.artConfig = ImageConfig(item.artUrl, null)
        setSelection(isSelected)
        itemInfoView.setOnClickListener {
            onSelectChange(item)
        }
    }

    override fun setSelection(isSelected: Boolean) {
        binding.selected = isSelected
    }

    override fun getCurrentId(): FilterItem? = binding.item
}