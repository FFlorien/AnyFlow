package be.florien.anyflow.feature.player.ui.songlist

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.PodcastEpisodeDisplay
import be.florien.anyflow.data.view.QueueItemDisplay
import be.florien.anyflow.databinding.ItemSongBinding
import be.florien.anyflow.extension.ImageConfig
import be.florien.anyflow.feature.player.ui.details.DetailViewHolder
import be.florien.anyflow.feature.player.ui.details.DetailViewHolderListener
import be.florien.anyflow.feature.player.ui.details.ItemInfoTouchAdapter
import be.florien.anyflow.feature.player.ui.info.InfoActions
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlin.math.absoluteValue


class QueueItemAdapter(
    val listener: SongListViewHolderListener,
    val provider: SongListViewHolderProvider,
    private val onSongClicked: (Int) -> Unit
) : PagingDataAdapter<QueueItemDisplay, SongViewHolder>(object :
    DiffUtil.ItemCallback<QueueItemDisplay>() {
    override fun areItemsTheSame(oldItem: QueueItemDisplay, newItem: QueueItemDisplay) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: QueueItemDisplay, newItem: QueueItemDisplay): Boolean =
        oldItem.artist == newItem.artist
                && oldItem.album == newItem.album
                && oldItem.title == newItem.title

}), FastScrollRecyclerView.SectionedAdapter {

    private var lastPosition = 0

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.isCurrentSong = position == provider.getCurrentPosition()
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SongViewHolder(parent, listener, provider, onSongClicked)

    fun setSelectedPosition(position: Int) {
        notifyItemChanged(lastPosition)
        notifyItemChanged(position)
        lastPosition = position
    }

    override fun getSectionName(position: Int): String = position.toString()
}

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
            binding.songActions.addView(
                (LayoutInflater.from(binding.songLayout.cover.context)
                    .inflate(R.layout.item_action, binding.songActions, false))
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
            shortcutListener.onShortcutOpened(absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION })
            val translationXEnd = binding.actionsPadding.right - itemView.width.toFloat()
            ObjectAnimator.ofFloat(binding.songLayout.songInfo, View.TRANSLATION_X, translationXEnd)
                .apply {
                    duration = 100L
                    interpolator = DecelerateInterpolator()
                    start()
                }
            startingTranslationX = translationXEnd
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

open class SongListTouchAdapter : ItemInfoTouchAdapter() {
    override fun onTouch(viewHolder: DetailViewHolder<*>, event: MotionEvent): Boolean {
        val parentOnTouch = super.onTouch(viewHolder, event)
        val isHandled = if (viewHolder !is SongViewHolder) {
            parentOnTouch
        } else if (!parentOnTouch && event.actionMasked == MotionEvent.ACTION_UP) {
            viewHolder.openShortcutWhenSwiped()
        } else {
            parentOnTouch
        }
        if (!isHandled && event.actionMasked == MotionEvent.ACTION_UP) {
            viewHolder.swipeToClose()
        }
        return isHandled
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