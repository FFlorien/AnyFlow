package be.florien.anyflow.common.ui.info

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter


val diffCallback = object :
    DiffUtil.ItemCallback<InfoRow>() { //todo this is not good: object might be shared across InfoAdapters
    override fun areItemsTheSame(
        oldItem: InfoRow,
        newItem: InfoRow
    ): Boolean = oldItem.areRowTheSame(newItem)

    override fun areContentsTheSame(
        oldItem: InfoRow,
        newItem: InfoRow
    ) = oldItem.areRowTheSame(newItem)
}

class InfoAdapter(private val executeAction: (InfoRow) -> Unit) :
    ListAdapter<InfoRow, InfoViewHolder>(diffCallback) {

    private val openedContainer = mutableSetOf<InfoRow.ContainerInfoRow>()

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is InfoRow.BasicInfoRow -> ItemViewType.ViewTypeBasic.value
            is InfoRow.ProgressInfoRow -> ItemViewType.ViewTypeProgress.value
            is InfoRow.ActionInfoRow,
            is InfoRow.NavigationInfoRow -> ItemViewType.ViewTypeAction.value

            is InfoRow.ContainerInfoRow -> ItemViewType.ViewTypeContainer.value
            is InfoRow.ShortcutInfoRow -> ItemViewType.ViewTypeShortcut.value
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder =
        when (ItemViewType.fromValue(viewType)) {
            ItemViewType.ViewTypeBasic -> InfoViewHolder.BasicInfoViewHolder(parent)
            ItemViewType.ViewTypeAction -> InfoViewHolder.ActionInfoViewHolder(
                parent,
                executeAction
            )

            ItemViewType.ViewTypeContainer -> InfoViewHolder.ContainerInfoViewHolder(
                parent,
                ::toggleContainer
            )

            ItemViewType.ViewTypeShortcut -> InfoViewHolder.ShortcutInfoViewHolder(
                parent,
                executeAction
            )

            ItemViewType.ViewTypeProgress -> InfoViewHolder.ProgressInfoViewHolder(
                parent,
                executeAction
            )
        }

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
            holder.bindNewData(getItem(position))
        }
    }

    private fun toggleContainer(infoRow: InfoRow) {
        if (infoRow !is InfoRow.ContainerInfoRow || infoRow.subRows.isEmpty()) {
            return
        }

        val nextList = currentList.toMutableList()
        val indexOfContainer = nextList.indexOf(infoRow)
        if (openedContainer.remove(infoRow)) {
            nextList.removeAll(infoRow.subRows)
        } else if (indexOfContainer >= 0) {
            openedContainer.add(infoRow)
            nextList.addAll(indexOfContainer + 1, infoRow.subRows)
        }

        super.submitList(nextList)
    }

    override fun submitList(list: List<InfoRow>?) {
        val nextList = syncNewListWithOpenedContainers(list)
        super.submitList(nextList)
    }

    override fun submitList(list: List<InfoRow>?, commitCallback: Runnable?) {
        val nextList = syncNewListWithOpenedContainers(list)
        super.submitList(nextList, commitCallback)
    }

    private fun syncNewListWithOpenedContainers(list: List<InfoRow>?): List<InfoRow>? {
        val nextList = list?.toMutableList()

        if (!nextList.isNullOrEmpty()) {
            for (item in nextList.reversed()) {
                if (item is InfoRow.ContainerInfoRow && openedContainer.contains(item)) {
                    nextList.addAll(nextList.indexOf(item) + 1, item.subRows)
                }
            }
        }

        return nextList
    }

    enum class ItemViewType(val value: Int) {
        ViewTypeBasic(0),
        ViewTypeAction(1),
        ViewTypeContainer(2),
        ViewTypeShortcut(3),
        ViewTypeProgress(4);

        companion object {
            fun fromValue(value: Int): ItemViewType =
                ItemViewType.entries.first { it.value == value }
        }
    }
}

