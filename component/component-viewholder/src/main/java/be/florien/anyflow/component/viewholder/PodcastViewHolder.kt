package be.florien.anyflow.component.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.component.viewholder.databinding.ItemPodcastBinding
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay

class PodcastViewHolder(
    parent: ViewGroup,
    listener: QueueItemViewHolderListener,
    provider: QueueItemViewHolderProvider,
    val binding: ItemPodcastBinding = ItemPodcastBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    ),
    shouldAlwaysShowShortcuts: Boolean = false
) : QueueItemViewHolder<PodcastEpisodeDisplay>(
    listener,
    provider,
    shouldAlwaysShowShortcuts,
    binding.root,
    binding.podcastLayout.podcastInfo,
    binding.infoView,
    binding.podcastActions
) {

    override val item: PodcastEpisodeDisplay?
        get() = binding.podcast

    /**
     * Public method
     */

    override fun bind(queueItem: PodcastEpisodeDisplay?) {
        binding.podcast = queueItem
        queueItem?.let {
            binding.art = ImageConfig(
                url = provider.getArtUrl(it),
                resource = R.drawable.cover_placeholder
            )
        }
        binding.podcastLayout.podcastInfo.translationX = if (isCurrent) {
            provider.getCurrentTranslationX()
        } else {
            0F
        }
        binding.podcastLayout.current = isCurrent
    }
}