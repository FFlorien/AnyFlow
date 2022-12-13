package be.florien.anyflow.feature.player.details

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.databinding.ItemSongBinding
import be.florien.anyflow.feature.player.info.InfoActions
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlin.math.absoluteValue


class SongAdapter(
    val listener: SongListViewHolderListener,
    val provider: SongListViewHolderProvider
) : PagingDataAdapter<SongInfo, SongViewHolder>(object : DiffUtil.ItemCallback<SongInfo>() {
    override fun areItemsTheSame(oldItem: SongInfo, newItem: SongInfo) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: SongInfo, newItem: SongInfo): Boolean =
        oldItem.artistName == newItem.artistName
                && oldItem.albumName == newItem.albumName
                && oldItem.title == newItem.title

}), FastScrollRecyclerView.SectionedAdapter {

    private var lastPosition = 0

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.isCurrentSong = position == provider.getCurrentPosition()
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SongViewHolder(parent, listener, provider)

    fun setSelectedPosition(position: Int) {
        notifyItemChanged(lastPosition)
        notifyItemChanged(position)
        lastPosition = position
    }

    override fun getSectionName(position: Int): String = position.toString()
}

class SongViewHolder(
    parent: ViewGroup,
    val listener: SongListViewHolderListener,
    val provider: SongListViewHolderProvider,
    internal val binding: ItemSongBinding = ItemSongBinding
        .inflate(LayoutInflater.from(parent.context), parent, false)
) : RecyclerView.ViewHolder(binding.root) {

    private var startingTranslationX: Float = 0f
    var isCurrentSong: Boolean = false

    init {
        if (parent is RecyclerView) {
            binding.songLayout.songInfo.setOnClickListener {
                binding.songLayout.songInfo.translationX = 0F
                listener.onItemClick(absoluteAdapterPosition)
            }
        }
        binding.songLayout.songInfo.setOnLongClickListener {
            swipeForInfo()
            return@setOnLongClickListener true
        }
        setQuickActions()
    }

    internal fun setQuickActions() {
        val childCount = binding.songActions.childCount
        if (childCount > 2) {
            binding.songActions.removeViews(2, childCount - 2)
        }
        val newActions = provider.getQuickActions()
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
                                listener.onQuickAction(
                                    song,
                                    action.actionType,
                                    action.fieldType
                                )
                            swipeToClose()
                        }
                    })
        }
    }

    fun bind(song: SongInfo?) {
        binding.song = song
        binding.art = song?.albumId?.let { provider.getArtUrl(it) }
        binding.songLayout.songInfo.translationX = if (isCurrentSong) {
            provider.getCurrentSongTranslationX()
        } else {
            0F
        }
        binding.songLayout.current = isCurrentSong
    }

    fun openInfoWhenSwiped() {
        if (
            binding.root.visibility == View.VISIBLE
            && binding.songLayout.songInfo.translationX > binding.infoView.right - 10
            && startingTranslationX == 0f
        ) {
            val song = binding.song ?: return
            listener.onInfoDisplayAsked(song)
        }
    }

    fun swipeToClose() {
        ObjectAnimator.ofFloat(binding.songLayout.songInfo, View.TRANSLATION_X, 0f).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(binding.songActions, View.TRANSLATION_X, 0f).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()
            start()
        }
        if (isCurrentSong && absoluteAdapterPosition != RecyclerView.NO_POSITION) {
            listener.onCurrentSongQuickActionClosed()
        }
        startingTranslationX = 0F
    }

    fun swipeToOpen() {
        listener.onQuickActionOpened(absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION })
        val translationXEnd = binding.actionsPadding.right - itemView.width.toFloat()
        ObjectAnimator.ofFloat(binding.songLayout.songInfo, View.TRANSLATION_X, translationXEnd)
            .apply {
                duration = 100L
                interpolator = DecelerateInterpolator()
                start()
            }
        startingTranslationX = translationXEnd
    }

    fun swipeForMove(translateX: Float) {
        val isQuickActionEmpty = provider.getQuickActions().isEmpty()
        if (translateX > 0F) {
            val translationToSeeActions = (binding.infoView.right).toFloat()
            val translationToFollowMove = startingTranslationX + translateX
            binding.songLayout.songInfo.translationX =
                minOf(translationToSeeActions, translationToFollowMove)
                    .coerceAtLeast(startingTranslationX)
        } else if (!isQuickActionEmpty) {
            val translationToSeeActions =
                (binding.actionsPadding.right - itemView.width).toFloat()
            val translationToFollowMove = startingTranslationX + translateX
            binding.songLayout.songInfo.translationX =
                maxOf(translationToSeeActions, translationToFollowMove).coerceAtMost(0f)
        }
    }

    private fun swipeForInfo() {
        ObjectAnimator.ofFloat(
            binding.songLayout.songInfo,
            View.TRANSLATION_X,
            binding.infoView.right.toFloat()
        ).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            repeatCount = 1
            repeatMode = ValueAnimator.REVERSE
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {}

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {
                    val song = binding.song ?: return
                    listener.onInfoDisplayAsked(song)
                }
            })
            start()
        }
    }
}

abstract class SongInfoTouchAdapter {
    internal var downTouchX: Float = -1f
    internal var downTouchY: Float = -1f
    private var lastTouchX: Float = -1f
    protected var lastDeltaX: Float = -1f
    protected var hasSwiped: Boolean = false

    protected fun onTouch(viewHolder: SongViewHolder, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                viewHolder.swipeForMove(event.x - downTouchX)
            }
            MotionEvent.ACTION_UP -> {
                if (lastDeltaX < -1.0) {
                    viewHolder.swipeToOpen()
                } else {
                    viewHolder.openInfoWhenSwiped()
                    viewHolder.swipeToClose()
                }
            }
        }
    }

    protected fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downTouchX = e.x
                lastTouchX = e.x
                downTouchY = e.y
                hasSwiped = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = downTouchX - e.x
                val deltaY = downTouchY - e.y
                hasSwiped = hasSwiped || deltaX.absoluteValue > deltaY.absoluteValue
                lastDeltaX = e.x - lastTouchX
                lastTouchX = e.x
                return hasSwiped
            }
            MotionEvent.ACTION_UP -> {
                val stopSwipe = hasSwiped
                downTouchX = -1f
                downTouchY = -1f
                lastTouchX = -1f
                lastDeltaX = -1f
                return stopSwipe
            }
        }
        return false
    }
}

interface DetailViewHolderListener<T> {
    fun onItemClick(position: Int)
    fun onInfoDisplayAsked(item: T)
}

interface SongListViewHolderListener : DetailViewHolderListener<SongInfo> {
    fun onQuickAction(item: SongInfo, action: InfoActions.ActionType, field: InfoActions.FieldType)
    fun onQuickActionOpened(position: Int?)
    fun onCurrentSongQuickActionClosed()
}

interface DetailViewHolderProvider {
    fun getArtUrl(id: Long): String
}

interface SongListViewHolderProvider : DetailViewHolderProvider {
    fun getQuickActions(): List<InfoActions.InfoRow>
    fun getCurrentPosition(): Int
    fun getCurrentSongTranslationX(): Float
}