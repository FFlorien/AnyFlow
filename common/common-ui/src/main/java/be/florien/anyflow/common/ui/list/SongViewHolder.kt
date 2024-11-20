package be.florien.anyflow.common.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.children
import be.florien.anyflow.common.ui.R
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.databinding.ItemSongBinding
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.QueueItemDisplay


class SongViewHolder(
    parent: ViewGroup,
    private val listener: SongListViewHolderListener,
    private val provider: SongListViewHolderProvider,
    val binding: ItemSongBinding = ItemSongBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    ),
    private val shouldAlwaysShowShortcuts: Boolean = false
) : ShortcutsViewHolder(
    binding.root,
    binding.songLayout.songInfo,
    binding.infoView,
    binding.songActions,
    provider::getShortcuts
) {

    val item: QueueItemDisplay?
        get() = binding.song
    var isCurrentSong: Boolean = false

    init {
        topView.setOnClickListener {
            if (!shouldAlwaysShowShortcuts)
                topView.translationX = 0F
            listener.onItemClick(absoluteAdapterPosition)
        }
        if (shouldAlwaysShowShortcuts) {
            shortcutsContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                openShortcuts()
            }
        }
    }

    /**
     * Public method
     */

    fun bind(item: QueueItemDisplay?) {
        binding.song = item
        item?.albumId?.let {
            binding.art = ImageConfig(
                url = provider.getArtUrl(it, item is PodcastEpisodeDisplay),
                resource = R.drawable.cover_placeholder
            )

        }
        binding.songLayout.songInfo.translationX = if (isCurrentSong) {
            provider.getCurrentSongTranslationX()
        } else {
            0F
        }
        binding.songLayout.current = isCurrentSong
    }

    /**
     * Overridden methods
     */

    override fun swipeAction() {
        item?.let {
            listener.onInfoDisplayAsked(it)
        }
    }

    override fun View.bindShortcut(action: Any) {
        if (action !is BaseSongInfoRow) {
            return
        }
        findViewById<ImageView>(R.id.action).setImageResource(action.actionType.iconRes)
        findViewById<ImageView>(R.id.field).setImageResource(action.fieldType.iconRes)
        setOnClickListener {
            val song = binding.song
            if (song != null)
                listener.onShortcut(song, action)
            if (!shouldAlwaysShowShortcuts)
                resetSwipePosition()
        }
    }

    override fun performDefaultShortcut() {
        val song = binding.song
        if (song != null) {
            binding.songActions.children.last().performClick()
        }
    }

    override fun onShortcutOpened(position: Int?) {
        listener.onShortcutOpened(position)
    }
}

interface SongListViewHolderListener {
    fun onItemClick(position: Int)
    fun onShortcut(item: QueueItemDisplay, row: BaseSongInfoRow)
    fun onCurrentSongShortcutsClosed()
    fun onInfoDisplayAsked(item: QueueItemDisplay)
    fun onShortcutOpened(position: Int?)
}

interface SongListViewHolderProvider {
    fun getCurrentPosition(): Int
    fun getCurrentSongTranslationX(): Float
    fun getArtUrl(id: Long, isPodcast: Boolean): String
    fun getShortcuts(): List<BaseSongInfoRow>
}