package be.florien.anyflow.feature.songlist.ui

import android.view.MotionEvent
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import be.florien.anyflow.component.viewholder.ErrorViewHolder
import be.florien.anyflow.component.viewholder.SwipeActionViewHolder
import be.florien.anyflow.component.viewholder.ItemInfoTouchAdapter
import be.florien.anyflow.component.viewholder.PodcastViewHolder
import be.florien.anyflow.component.viewholder.PodcastViewHolderListener
import be.florien.anyflow.component.viewholder.PodcastViewHolderProvider
import be.florien.anyflow.component.viewholder.QueueItemViewHolder
import be.florien.anyflow.component.viewholder.QueueItemViewHolderListener
import be.florien.anyflow.component.viewholder.QueueItemViewHolderProvider
import be.florien.anyflow.component.viewholder.SongViewHolder
import be.florien.anyflow.management.queue.model.ErrorDisplay
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.QueueItemDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView


val diffCallback = object :
    DiffUtil.ItemCallback<QueueItemDisplay>() {
    override fun areItemsTheSame(oldItem: QueueItemDisplay, newItem: QueueItemDisplay) =
        when (oldItem) {
            is SongDisplay -> newItem is SongDisplay && oldItem.id == newItem.id
            is PodcastEpisodeDisplay -> newItem is PodcastEpisodeDisplay && oldItem.id == newItem.id
            ErrorDisplay -> true
        }

    override fun areContentsTheSame(oldItem: QueueItemDisplay, newItem: QueueItemDisplay): Boolean =
        when (oldItem) {
            is SongDisplay -> newItem is SongDisplay &&
                    oldItem.artistName == newItem.artistName
                    && oldItem.albumName == newItem.albumName
                    && oldItem.title == newItem.title

            is PodcastEpisodeDisplay -> newItem is PodcastEpisodeDisplay && oldItem.id == newItem.id
            ErrorDisplay -> true
        }

}

class QueueItemAdapter(
    private val queueItemListener: QueueItemViewHolderListener,
    private val queueItemProvider: QueueItemViewHolderProvider,
    private val podcastListener: PodcastViewHolderListener,
    private val podcastViewHolderProvider: PodcastViewHolderProvider
) : PagingDataAdapter<QueueItemDisplay, QueueItemViewHolder<*>>(diffCallback),
    FastScrollRecyclerView.SectionedAdapter {

    private var lastPosition = 0

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is SongDisplay -> ITEM_TYPE_SONG
        is PodcastEpisodeDisplay -> ITEM_TYPE_PODCAST
        ErrorDisplay -> ITEM_TYPE_ERROR
        null -> ITEM_TYPE_SONG
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            ITEM_TYPE_SONG -> SongViewHolder(parent, queueItemListener, queueItemProvider)
            ITEM_TYPE_PODCAST -> PodcastViewHolder(parent, queueItemListener, queueItemProvider, podcastListener, podcastViewHolderProvider, shouldShowTimeStamps = true)
            else -> ErrorViewHolder(parent,queueItemListener, queueItemProvider )
        }

    override fun onBindViewHolder(holder: QueueItemViewHolder<*>, position: Int) {
        holder.isCurrent = position == queueItemProvider.getCurrentPositionFor()
        when (holder) {
            is SongViewHolder -> {
                holder.bind(getItem(position) as? SongDisplay)
            }

            is PodcastViewHolder -> {
                holder.bind(getItem(position) as? PodcastEpisodeDisplay)
            }
        }
    }

    fun setSelectedPosition(position: Int) {
        notifyItemChanged(lastPosition)
        notifyItemChanged(position)
        lastPosition = position
    }

    override fun getSectionName(position: Int): String = position.toString()

    companion object {
        const val ITEM_TYPE_SONG = 0
        const val ITEM_TYPE_PODCAST = 1
        const val ITEM_TYPE_ERROR = 1
    }
}

open class SongListTouchAdapter : ItemInfoTouchAdapter() {
    override fun onTouch(viewHolder: SwipeActionViewHolder, event: MotionEvent): Boolean {
        val parentOnTouch = super.onTouch(viewHolder, event)
        val isHandled = if (viewHolder !is SongViewHolder) {
            parentOnTouch
        } else if (!parentOnTouch && event.actionMasked == MotionEvent.ACTION_UP) {
            viewHolder.openShortcutWhenSwiped()
        } else {
            parentOnTouch
        }
        if (!isHandled && event.actionMasked == MotionEvent.ACTION_UP) {
            viewHolder.resetSwipePosition()
        }
        return isHandled
    }
}