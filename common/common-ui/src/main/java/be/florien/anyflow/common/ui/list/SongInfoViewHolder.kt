package be.florien.anyflow.common.ui.list

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.common.ui.R
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.databinding.ItemSongBinding
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.QueueItemDisplay
import kotlin.math.absoluteValue


class SongViewHolder(
    parent: ViewGroup,
    listener: SongListViewHolderListener,
    val provider: SongListViewHolderProvider,
    private val onSongClicked: ((Int) -> Unit)?,
    val binding: ItemSongBinding = ItemSongBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    ),
    private val shouldAlwaysShowShortcuts: Boolean = false
) : DetailViewHolder<QueueItemDisplay>(listener, binding.root) {

    override val itemInfoView: View
        get() = binding.songLayout.songInfo
    override val infoIconView: View
        get() = binding.infoView
    override val item: QueueItemDisplay?
        get() = binding.song

    private val shortcutListener: SongListViewHolderListener
        get() = listener as SongListViewHolderListener

    var isCurrentSong: Boolean = false

    init {
        setShortcuts()
        setClickListener()
        itemInfoView.setOnClickListener {
            if (!shouldAlwaysShowShortcuts)
                itemInfoView.translationX = 0F
            onSongClicked?.invoke(absoluteAdapterPosition)
        }
        if (shouldAlwaysShowShortcuts) {
            binding.songActions.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                openShortcuts()
            }
        }
    }

    /**
     * Overridden methods
     */

    override fun swipeForMove(translateX: Float): Boolean {
        if (!super.swipeForMove(translateX) && provider.getShortcuts().isNotEmpty()) {
            val translationToSeeActions = (binding.actionsPadding.right - itemView.width).toFloat()
            val translationToFollowMove = startingTranslationX + translateX
            binding.songLayout.songInfo.translationX =
                maxOf(translationToSeeActions, translationToFollowMove).coerceAtMost(0f)
            return true
        }
        return false
    }

    override fun swipeToClose() {
        super.swipeToClose()
        if (isCurrentSong && absoluteAdapterPosition != RecyclerView.NO_POSITION) {
            shortcutListener.onCurrentSongShortcutsClosed()
        }
    }

    /**
     * Public methods
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

    fun setShortcuts() {
        val childCount = binding.songActions.childCount
        if (childCount > 2) {
            binding.songActions.removeViews(2, childCount - 2)
        }
        val newActions = provider.getShortcuts().reversed()
        for (action in newActions) {
            val itemAction = if (newActions.size == 1) {
                R.layout.item_action_unique
            } else {
                R.layout.item_action
            }
            binding.songActions.addView(
                (LayoutInflater.from(binding.songLayout.cover.context)
                    .inflate(itemAction, binding.songActions, false))
                    .apply {
                        findViewById<ImageView>(R.id.action).setImageResource(action.actionType.iconRes)
                        findViewById<ImageView>(R.id.field).setImageResource(action.fieldType.iconRes)
                        setOnClickListener {
                            val song = binding.song
                            if (song != null)
                                shortcutListener.onShortcut(song, action)
                            if (!shouldAlwaysShowShortcuts)
                                swipeToClose()
                        }
                    })
        }
    }

    fun openShortcutWhenSwiped(): Boolean {
        val shortcutsWidth = binding.actionsPadding.right - binding.songActions.right
        return if (itemInfoView.translationX < shortcutsWidth + (shortcutsWidth.absoluteValue / 4)) {
            if (binding.songActions.childCount == 3) {
                val song = binding.song
                if (song != null) {
                    binding.songActions.children.last().performClick()
                }
                swipeToClose()
            } else {
                shortcutListener.onShortcutOpened(absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION })
                val translationXEnd = binding.actionsPadding.right - itemView.width.toFloat()
                ObjectAnimator
                    .ofFloat(
                        binding.songLayout.songInfo,
                        View.TRANSLATION_X,
                        translationXEnd
                    )
                    .apply {
                        duration = 100L
                        interpolator = DecelerateInterpolator()
                        start()
                    }
                startingTranslationX = translationXEnd
            }
            true
        } else {
            false
        }
    }

    private fun openShortcuts() {
        val translationXEnd = binding.actionsPadding.right - itemView.width.toFloat()
        binding.songLayout.songInfo.translationX = translationXEnd
        startingTranslationX = translationXEnd
    }
}

interface SongListViewHolderListener : DetailViewHolderListener<QueueItemDisplay> {
    fun onShortcut(item: QueueItemDisplay, row: InfoActions.InfoRow)
    fun onShortcutOpened(position: Int?)
    fun onCurrentSongShortcutsClosed()
}

interface SongListViewHolderProvider {
    fun getShortcuts(): List<InfoActions.InfoRow>
    fun getCurrentPosition(): Int
    fun getCurrentSongTranslationX(): Float
    fun getArtUrl(id: Long, isPodcast: Boolean): String
}