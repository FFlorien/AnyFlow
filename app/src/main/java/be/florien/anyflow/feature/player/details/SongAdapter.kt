package be.florien.anyflow.feature.player.details

import android.animation.ObjectAnimator
import android.view.LayoutInflater
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
    listener: SongListViewHolderListener,
    val provider: SongListViewHolderProvider,
    binding: ItemSongBinding = ItemSongBinding
        .inflate(LayoutInflater.from(parent.context), parent, false)
) : DetailViewHolder<SongInfo, ItemSongBinding, SongListViewHolderListener>(
    parent,
    listener,
    binding
) {

    override val itemInfoView: View
        get() = binding.songLayout.songInfo
    override val infoView: View
        get() = binding.infoView
    override val item: SongInfo?
        get() = binding.song

    var isCurrentSong: Boolean = false

    init {
        setQuickActions()
    }

    override fun swipeForMove(translateX: Float): Boolean {
        if (!super.swipeForMove(translateX) && provider.getQuickActions().isNotEmpty()) {
            val translationToSeeActions =
                (binding.actionsPadding.right - itemView.width).toFloat()
            val translationToFollowMove = startingTranslationX + translateX
            binding.songLayout.songInfo.translationX =
                maxOf(translationToSeeActions, translationToFollowMove).coerceAtMost(0f)
            return true
        }
        return false
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
                            swipeToCloseQuickActions()
                        }
                    })
        }
    }

    override fun bind(item: SongInfo?) {
        binding.song = item
        binding.art = item?.albumId?.let { provider.getArtUrl(it) }
        binding.songLayout.songInfo.translationX = if (isCurrentSong) {
            provider.getCurrentSongTranslationX()
        } else {
            0F
        }
        binding.songLayout.current = isCurrentSong
    }

    fun swipeToOpenQuickActions() {
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

    fun swipeToCloseQuickActions() {
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