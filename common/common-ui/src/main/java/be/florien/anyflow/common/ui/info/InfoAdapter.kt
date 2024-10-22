package be.florien.anyflow.common.ui.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.common.ui.R
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.common.ui.databinding.ItemInfoBinding
import be.florien.anyflow.common.ui.databinding.ItemProgressInfoBinding
import be.florien.anyflow.common.ui.databinding.ItemShortcutInfoBinding


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

    class BasicInfoViewHolder(
        parent: ViewGroup
    ) : InfoViewHolder(parent, {})

    class ContainerInfoViewHolder(
        parent: ViewGroup,
        executeAction: (InfoRow) -> Unit
    ) : InfoViewHolder(parent, executeAction)

    class ActionInfoViewHolder(
        parent: ViewGroup,
        executeAction: (InfoRow) -> Unit
    ) : InfoViewHolder(parent, executeAction)


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

sealed class InfoRow(
    open @StringRes val title: Int,
    open val text: TextConfig,
    open val image: ImageConfig,
    open @DrawableRes val icon: Int?,
    open val tag: Any
) {
    open fun areRowTheSame(other: InfoRow): Boolean {
        return text == other.text && (image == other.image) && this.javaClass == other.javaClass
    }

    data class BasicInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any
    ) :
        InfoRow(title, text, image, null, tag)

    data class ProgressInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any,
        val progress: LiveData<Double>,
        val secondaryProgress: LiveData<Double>
    ) : InfoRow(title, text, image, null, tag) {
        override fun equals(other: Any?) =
            other is ProgressInfoRow && title == other.title && text == other.text && image == other.image && tag == other.tag

        override fun hashCode() =
            title.hashCode() + text.hashCode() + image.hashCode() + tag.hashCode()
    }

    data class ActionInfoRow(override val title: Int, override val text: TextConfig, override val image: ImageConfig, override val tag: Any) :
        InfoRow(title, text, image, null, tag)

    data class NavigationInfoRow(override val title: Int, override val text: TextConfig, override val image: ImageConfig, override val tag: Any) :
        InfoRow(title, text, image, R.drawable.ic_go, tag)

    data class ContainerInfoRow(
        override val title: Int,
        override val text: TextConfig,
        override val image: ImageConfig,
        override val tag: Any,
        val subRows: List<InfoRow>
    ) : InfoRow(title, text, image, R.drawable.ic_next_occurence, tag)

    data class ShortcutInfoRow(override val title: Int, override val text: TextConfig, override val image: ImageConfig, override val tag: Any) :
        InfoRow(title, text, image, null, tag)
}
