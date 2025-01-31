package be.florien.anyflow.component.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import be.florien.anyflow.component.viewholder.databinding.ItemErrorBinding
import be.florien.anyflow.component.viewholder.databinding.ItemPodcastChapterBinding
import be.florien.anyflow.management.queue.model.ErrorDisplay

class ErrorViewHolder(
    val parent: ViewGroup,
    listener: QueueItemViewHolderListener,
    provider: QueueItemViewHolderProvider,
    val binding: ItemErrorBinding = ItemErrorBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    ),
    shouldAlwaysShowShortcuts: Boolean = false,
) : QueueItemViewHolder<ErrorDisplay>(
    listener,
    provider,
    shouldAlwaysShowShortcuts,
    binding.root,
    binding.textLayout,
    binding.infoView,
    binding.podcastActions
) {

    override val item = ErrorDisplay
    private val chaptersBindings = mutableListOf<ItemPodcastChapterBinding>()

    /**
     * Public method
     */

    override fun bind(queueItem: ErrorDisplay?) {
    }
}