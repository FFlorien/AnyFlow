package be.florien.anyflow.component.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.size
import androidx.lifecycle.LiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.component.viewholder.databinding.ItemPodcastBinding
import be.florien.anyflow.component.viewholder.databinding.ItemPodcastChapterBinding
import be.florien.anyflow.management.queue.model.Chapter
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay

class PodcastViewHolder(
    val parent: ViewGroup,
    listener: QueueItemViewHolderListener,
    provider: QueueItemViewHolderProvider,
    private val podcastListener: PodcastViewHolderListener,
    private val podcastViewHolderProvider: PodcastViewHolderProvider,
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
    private val chaptersBindings = mutableListOf<ItemPodcastChapterBinding>()

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
        chaptersBindings.clear()

        if (shouldShowTimeStamps && isCurrent && queueItem?.chapters?.isNotEmpty() == true) {
            binding.setLifecycleOwner(parent.findViewTreeLifecycleOwner())
            parent.findViewTreeLifecycleOwner()?.let { lifecycleOwner ->
                podcastViewHolderProvider.chapterObservable.observe(lifecycleOwner) { currentChapter ->
                    val currentlySelectedBinding = chaptersBindings
                        .firstOrNull { it.current == true && it.chapter != currentChapter }
                    currentlySelectedBinding?.current = false
                    val chapterBinding = chaptersBindings
                        .firstOrNull { it.chapter == currentChapter }
                    chapterBinding?.current = true
                }
            }

            queueItem.chapters.forEach { itemChapter ->
                val chapterBinding = ItemPodcastChapterBinding.inflate(
                    LayoutInflater.from(binding.root.context),
                    binding.container,
                    true
                )

                chapterBinding.chapter = itemChapter
                chapterBinding.current = false
                chapterBinding.root.setOnClickListener {
                    podcastListener.onChapterClicked(itemChapter.time)
                }

                chaptersBindings.add(chapterBinding)
            }
        }
    }
}

interface PodcastViewHolderListener {
    fun onChapterClicked(time: Long)
}

interface PodcastViewHolderProvider {
    val chapterObservable: LiveData<Chapter?>
}