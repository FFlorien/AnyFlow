package be.florien.anyflow.component.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import be.florien.anyflow.component.viewholder.databinding.ItemErrorBinding
import be.florien.anyflow.feature.songlist.base.domain.model.QueueItemInfoRow
import be.florien.anyflow.management.queue.model.ErrorDisplay
import be.florien.anyflow.management.queue.model.QueueItemDisplay

class ErrorViewHolder(
    val parent: ViewGroup,
    val binding: ItemErrorBinding = ItemErrorBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    )
) : QueueItemViewHolder<ErrorDisplay>(
    object : QueueItemViewHolderListener {
        override fun onItemClick(position: Int) {}
        override fun onShortcut(item: QueueItemDisplay, row: QueueItemInfoRow<*, *>) {}
        override fun onCurrentShortcutsClosed() {}
        override fun onInfoDisplayAsked(item: QueueItemDisplay) {}
        override fun onShortcutOpened(position: Int?) {}
    },
    object : QueueItemViewHolderProvider {
        override fun getCurrentPositionFor(): Int = 0
        override fun getCurrentTranslationX(): Float = 0.0f
        override fun getArtUrl(item: QueueItemDisplay): String = ""
        override fun getShortcuts(): List<QueueItemInfoRow<*, *>> = emptyList()
    },
    false,
    binding.root,
    binding.textLayout,
    binding.infoView,
    binding.podcastActions
) {

    override val item = ErrorDisplay

    /**
     * Public method
     */

    override fun bind(queueItem: ErrorDisplay?) {
    }
}