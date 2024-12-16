package be.florien.anyflow.component.viewholder

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.size
import be.florien.anyflow.common.base.setHtmlText
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.component.viewholder.databinding.ItemPodcastBinding
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay

class PodcastViewHolder(
    parent: ViewGroup,
    listener: QueueItemViewHolderListener,
    provider: QueueItemViewHolderProvider,
    private val podcastListener: PodcastViewHolderListener,
    val binding: ItemPodcastBinding = ItemPodcastBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    ),
    val shouldShowTimeStamps: Boolean,
    shouldAlwaysShowShortcuts: Boolean = false,
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
        binding.container.removeViews(1, binding.container.size - 1)
        if (shouldShowTimeStamps && isCurrent && queueItem?.timeStamps?.isNotEmpty() == true) {
            queueItem.timeStamps.forEach { timeStamp ->
                binding.container.addView(
                    TextView(binding.container.context).apply {
                        setHtmlText(timeStamp.text)
                        setLinkTextColor(Color.BLUE)
                        setBackgroundResource(R.drawable.bg_action)
                        setOnClickListener {
                            podcastListener.onTimeStampClicked(timeStamp.time)
                        }
                    }
                )
            }
        }
    }
}

interface PodcastViewHolderListener {
    fun onTimeStampClicked(time: Long)
}