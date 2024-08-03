package be.florien.anyflow.common.ui.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.common.ui.R
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.databinding.ItemInfoBinding


abstract class InfoAdapter<VH : InfoViewHolder> : ListAdapter<InfoActions.InfoRow, VH>(object :
    DiffUtil.ItemCallback<InfoActions.InfoRow>() {
    override fun areItemsTheSame(
        oldItem: InfoActions.InfoRow,
        newItem: InfoActions.InfoRow
    ): Boolean = oldItem.areRowTheSame(newItem)

    override fun areContentsTheSame(
        oldItem: InfoActions.InfoRow,
        newItem: InfoActions.InfoRow
    ) = oldItem.areContentTheSame(newItem) && newItem.areContentTheSame(oldItem)
}) {

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bindNewData(getItem(position))
        holder.setLifecycleOwner()
    }

    override fun onBindViewHolder(
        holder: VH,
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

open class InfoViewHolder(
    protected val parent: ViewGroup,
    private val executeAction: (row: InfoActions.InfoRow) -> Unit,
    protected val binding: ItemInfoBinding = ItemInfoBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    ),
    root: View = binding.root
) : RecyclerView.ViewHolder(root) {

    fun bindNewData(row: InfoActions.InfoRow) {
        bindChangedData(row)
        val text = row.text
        val textRes = row.textRes
        val resources = parent.context.resources
        //variable
        binding.display = row
        binding.descriptionText = when {
            text == null && textRes != null -> resources.getString(textRes)
            text != null && textRes == null -> text
            text != null && textRes != null -> resources.getString(textRes, text)
            else -> ""
        }
        binding.imageConfig =
            if (row.actionType.category != InfoActions.ActionTypeCategory.Action) {
                ImageConfig(row.imageUrl, row.fieldType.iconRes)
            } else {
                ImageConfig(null, null)
            }
        //onClick
        itemView.setOnClickListener {
            executeAction(row)
        }
        //background
        when (row.actionType.category) {
            InfoActions.ActionTypeCategory.None -> itemView.background = null
            InfoActions.ActionTypeCategory.Navigation -> itemView.setBackgroundResource(R.drawable.bg_transparent_ripple)
            InfoActions.ActionTypeCategory.Action -> itemView.setBackgroundResource(R.drawable.bg_yellow_selectable_ripple)
        }
    }

    open fun setLifecycleOwner() {
        binding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
    }

    open fun bindChangedData(row: InfoActions.InfoRow) {
    }
}
