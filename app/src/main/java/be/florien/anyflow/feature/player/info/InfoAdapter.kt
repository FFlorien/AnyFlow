package be.florien.anyflow.feature.player.info

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.size
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ItemInfoBinding
import be.florien.anyflow.extension.ImageConfig


class InfoAdapter(
    private val executeAction: (InfoActions.FieldType, InfoActions.ActionType) -> Unit
) : ListAdapter<InfoActions.InfoRow, InfoViewHolder>(object :
    DiffUtil.ItemCallback<InfoActions.InfoRow>() {
    override fun areItemsTheSame(
        oldItem: InfoActions.InfoRow,
        newItem: InfoActions.InfoRow
    ): Boolean {
        val isSameAction = (oldItem.actionType == newItem.actionType)
                || (oldItem.actionType is InfoActions.ActionType.ExpandedTitle && newItem.actionType is InfoActions.ActionType.ExpandableTitle)
                || (oldItem.actionType is InfoActions.ActionType.ExpandableTitle && newItem.actionType is InfoActions.ActionType.ExpandedTitle)
        return oldItem.fieldType == newItem.fieldType && isSameAction
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(
        oldItem: InfoActions.InfoRow,
        newItem: InfoActions.InfoRow
    ) = areItemsTheSame(
        oldItem,
        newItem
    ) && oldItem.actionType == newItem.actionType && oldItem.additionalInfo == newItem.additionalInfo && oldItem.progress === newItem.progress
}) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder =
        InfoViewHolder(parent, executeAction)

    override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
        holder.bindNewData(getItem(position))
    }

    override fun onBindViewHolder(
        holder: InfoViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.bindChangedData(getItem(position))
        }
    }
}

class InfoViewHolder(
    private val parent: ViewGroup,
    private val executeAction: (InfoActions.FieldType, InfoActions.ActionType) -> Unit,
    private val binding: ItemInfoBinding = ItemInfoBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bindNewData(row: InfoActions.InfoRow) {
        bindChangedData(row)
        binding.display = row
        binding.descriptionText = when {
            row.text == null && row.textRes != null -> parent.context.resources.getString(row.textRes)
            row.text != null && row.textRes == null -> row.text
            row.text != null && row.textRes != null -> parent.context.resources.getString(
                row.textRes,
                row.text
            )
            else -> ""
        }
        binding.root.setOnClickListener {
            executeAction(row.fieldType, row.actionType)
        }
        if (row.actionType is InfoActions.ActionType.None) {
            binding.root.setBackgroundColor(
                ResourcesCompat.getColor(
                    parent.context.resources,
                    R.color.primaryBackground,
                    parent.context.theme
                )
            )
        } else if (
            row.actionType !is InfoActions.ActionType.InfoTitle
            && row.actionType !is InfoActions.ActionType.ExpandableTitle
            && row.actionType !is InfoActions.ActionType.ExpandedTitle
            && row.actionType !is InfoActions.LibraryActionType.SubFilter
        ) {
            binding.root.setBackgroundResource(R.drawable.bg_yellow_selectable_ripple)
        } else {
            binding.root.setBackgroundResource(R.drawable.bg_blue_light_selectable_ripple)
        }
        binding.imageConfig = if (row.actionType !is InfoActions.SongActionType) {
            row.fieldType.imageConfig
        } else {
            ImageConfig(null, null)
        }
        binding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
    }

    fun bindChangedData(row: InfoActions.InfoRow) {
        if (row.additionalInfo != null) {
            binding.order.removeViews(2, binding.order.size - 2)
            val inflater = LayoutInflater.from(binding.root.context)
            for (i in 0 until row.additionalInfo) {
                val unselectedView = inflater
                    .inflate(R.layout.item_action_order, binding.order, false) as ImageView
                unselectedView.setImageResource(R.drawable.ic_action_order_item_unselected)
                binding.order.addView(unselectedView)
            }
        }
    }
}
