package be.florien.anyflow.feature.player.details

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
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.databinding.ItemSongBinding
import be.florien.anyflow.extension.ImageConfig
import be.florien.anyflow.feature.player.info.InfoActions
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlin.math.absoluteValue


class SongAdapter(
    val listener: SongListViewHolderListener,
    val provider: SongListViewHolderProvider,
    private val onSongClicked: (Int) -> Unit
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
    )
) : DetailViewHolder<SongInfo>(listener, binding.root) {

    override val itemInfoView: View
        get() = binding.songLayout.songInfo
    override val infoIconView: View
        get() = binding.infoView
    override val item: SongInfo?
        get() = binding.song

    private val quickActionListener: SongListViewHolderListener
        get() = listener as SongListViewHolderListener

    var isCurrentSong: Boolean = false

    init {
        setQuickActions()
        setClickListener()
        itemInfoView.setOnClickListener {
            itemInfoView.translationX = 0F
            onSongClicked?.invoke(absoluteAdapterPosition)
        }
    }

    /**
     * Overridden methods
     */

    override fun swipeForMove(translateX: Float): Boolean {
        if (!super.swipeForMove(translateX) && provider.getQuickActions().isNotEmpty()) {
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
            quickActionListener.onCurrentSongQuickActionClosed()
        }
    }

    /**
     * Public methods
     */

    fun bind(item: SongInfo?) {
        binding.song = item
        binding.art = ImageConfig(
            url = item?.albumId?.let { provider.getArtUrl(it) },
            resource = R.drawable.cover_placeholder
        )
        binding.songLayout.songInfo.translationX = if (isCurrentSong) {
            provider.getCurrentSongTranslationX()
        } else {
            0F
        }
        binding.songLayout.current = isCurrentSong
    }

    fun setQuickActions() {
        val childCount = binding.songActions.childCount
        if (childCount > 2) {
            binding.songActions.removeViews(2, childCount - 2)
        }
        val newActions = provider.getQuickActions().reversed()
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
                                quickActionListener.onQuickAction(
                                    song,
                                    action.actionType,
                                    action.fieldType
                                )
                            swipeToClose()
                        }
                    })
        }
    }

    fun openQuickActionWhenSwiped(): Boolean {
        val quickActionsWidth = binding.actionsPadding.right - binding.songActions.right
        return if (itemInfoView.translationX < quickActionsWidth + (quickActionsWidth.absoluteValue / 4)) {
            quickActionListener.onQuickActionOpened(absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION })
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
}

open class SongListTouchAdapter : ItemInfoTouchAdapter() {
    override fun onTouch(viewHolder: DetailViewHolder<*>, event: MotionEvent): Boolean {
        val parentOnTouch = super.onTouch(viewHolder, event)
        val isHandled = if (viewHolder !is SongViewHolder) {
            parentOnTouch
        } else if (!parentOnTouch && event.actionMasked == MotionEvent.ACTION_UP) {
            viewHolder.openQuickActionWhenSwiped()
        } else {
            parentOnTouch
        }
        if (!isHandled && event.actionMasked == MotionEvent.ACTION_UP) {
            viewHolder.swipeToClose()
        }
        return isHandled
    }
}

interface SongListViewHolderListener : DetailViewHolderListener<SongInfo> {
    fun onQuickAction(item: SongInfo, action: InfoActions.ActionType, field: InfoActions.FieldType)
    fun onQuickActionOpened(position: Int?)
    fun onCurrentSongQuickActionClosed()
}

interface SongListViewHolderProvider {
    fun getQuickActions(): List<InfoActions.InfoRow>
    fun getCurrentPosition(): Int
    fun getCurrentSongTranslationX(): Float
    fun getArtUrl(id: Long): String
}