package be.florien.anyflow.component.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.component.viewholder.databinding.ItemSongBinding
import be.florien.anyflow.management.queue.model.SongDisplay


class SongViewHolder(
    parent: ViewGroup,
    listener: QueueItemViewHolderListener,
    provider: QueueItemViewHolderProvider,
    val binding: ItemSongBinding = ItemSongBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    ),
    shouldAlwaysShowShortcuts: Boolean = false
) : QueueItemViewHolder<SongDisplay>(
    listener,
    provider,
    shouldAlwaysShowShortcuts,
    binding.root,
    binding.songLayout.songInfo,
    binding.infoView,
    binding.songActions
) {

    override val item: SongDisplay?
        get() = binding.song

    /**
     * Public method
     */

    override fun bind(queueItem: SongDisplay?) {
        binding.song = queueItem
        queueItem?.let {
            binding.art = ImageConfig(
                url = provider.getArtUrl(it),
                resource = R.drawable.cover_placeholder
            )
        }
        binding.songLayout.songInfo.translationX = if (isCurrent) {
            provider.getCurrentTranslationX()
        } else {
            0F
        }
        binding.songLayout.current = isCurrent
    }
}