package be.florien.anyflow.common.ui.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.common.ui.R
import be.florien.anyflow.common.ui.databinding.ItemInfoBinding
import be.florien.anyflow.common.ui.databinding.ItemProgressInfoBinding
import be.florien.anyflow.common.ui.databinding.ItemShortcutInfoBinding

sealed class InfoViewHolder(
    protected val parent: ViewGroup,
    private val executeAction: (row: InfoRow) -> Unit,
    protected val binding: ItemInfoBinding = ItemInfoBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    ),
    root: View = binding.root
) : RecyclerView.ViewHolder(root) {

    fun bindNewData(row: InfoRow) { //todo bind only if different ?
        val resources = parent.context.resources
        //variable
        binding.display = row
        binding.descriptionText = row.text.getText(resources)
        //onClick
        itemView.setOnClickListener {
            executeAction(row)
        }
        //background
        when (row) {
            is InfoRow.BasicInfoRow -> itemView.background = null

            is InfoRow.NavigationInfoRow,
            is InfoRow.ContainerInfoRow -> itemView.setBackgroundResource(R.drawable.bg_transparent_ripple)

            is InfoRow.ShortcutInfoRow,
            is InfoRow.ProgressInfoRow,
            is InfoRow.ActionInfoRow -> itemView.setBackgroundResource(R.drawable.bg_yellow_selectable_ripple)
        }
    }

    open fun bindChangedData(row: InfoRow) {}

    class BasicInfoViewHolder(parent: ViewGroup) :
        InfoViewHolder(parent, {})

    class ContainerInfoViewHolder(parent: ViewGroup, executeAction: (InfoRow) -> Unit) :
        InfoViewHolder(parent, executeAction)

    class ActionInfoViewHolder(parent: ViewGroup, executeAction: (InfoRow) -> Unit) :
        InfoViewHolder(parent, executeAction)

    class ShortcutInfoViewHolder(
        parent: ViewGroup,
        executeAction: (row: InfoRow) -> Unit,
        parentBinding: ItemShortcutInfoBinding = ItemShortcutInfoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ) : InfoViewHolder(parent, executeAction, parentBinding.infoLayout, parentBinding.root)

    class ProgressInfoViewHolder(
        parent: ViewGroup,
        executeAction: (row: InfoRow) -> Unit,
        private val parentBinding: ItemProgressInfoBinding = ItemProgressInfoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ) : InfoViewHolder(parent, executeAction, parentBinding.infoLayout, parentBinding.root) {

        override fun bindChangedData(row: InfoRow) {
            super.bindChangedData(row)
            if (row is InfoRow.ProgressInfoRow) {
                parent.findViewTreeLifecycleOwner()?.let {
                    parentBinding.progress.max = 100
                    row.progress.observe(it) {
                        parentBinding.progress.progress = (it * 100).toInt()
                    }
                    row.secondaryProgress.observe(it) {
                        parentBinding.progress.secondaryProgress = (it * 100).toInt()
                    }
                }
            }
        }
    }
}