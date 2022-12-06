package be.florien.anyflow.feature.player.info

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.size
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ItemInfoBinding


class InfoAdapter(
    private val executeAction: (InfoActions.FieldType, InfoActions.ActionType) -> Unit
) : ListAdapter<InfoActions.InfoRow, InfoViewHolder>(object :
    DiffUtil.ItemCallback<InfoActions.InfoRow>() {
    override fun areItemsTheSame(
        oldItem: InfoActions.InfoRow,
        newItem: InfoActions.InfoRow
    ): Boolean {
        val isSameAction = (oldItem.actionType == newItem.actionType)
                || (oldItem.actionType == InfoActions.ActionType.EXPANDED_TITLE && newItem.actionType == InfoActions.ActionType.EXPANDABLE_TITLE)
                || (oldItem.actionType == InfoActions.ActionType.EXPANDABLE_TITLE && newItem.actionType == InfoActions.ActionType.EXPANDED_TITLE)
        return oldItem.fieldType == newItem.fieldType && isSameAction
    }

    override fun areContentsTheSame(
        oldItem: InfoActions.InfoRow,
        newItem: InfoActions.InfoRow
    ) = areItemsTheSame(
        oldItem,
        newItem
    ) && oldItem.actionType == newItem.actionType && oldItem.additionalInfo == newItem.additionalInfo
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
        if (row.actionType == InfoActions.ActionType.NONE) {
            binding.root.setBackgroundColor(
                ResourcesCompat.getColor(
                    parent.context.resources,
                    R.color.primaryBackground,
                    parent.context.theme
                )
            )
        } else if (row.actionType != InfoActions.ActionType.INFO_TITLE && row.actionType != InfoActions.ActionType.EXPANDABLE_TITLE && row.actionType != InfoActions.ActionType.EXPANDED_TITLE) {
            binding.root.setBackgroundResource(R.drawable.bg_yellow_selectable_ripple)
        } else {
            binding.root.setBackgroundResource(R.drawable.bg_blue_light_selectable_ripple)
        }
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
