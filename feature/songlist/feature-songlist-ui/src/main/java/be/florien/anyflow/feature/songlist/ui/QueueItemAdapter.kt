package be.florien.anyflow.feature.songlist.ui

import android.view.MotionEvent
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import be.florien.anyflow.common.ui.list.DetailViewHolder
import be.florien.anyflow.common.ui.list.ItemInfoTouchAdapter
import be.florien.anyflow.common.ui.list.SongListViewHolderListener
import be.florien.anyflow.common.ui.list.SongListViewHolderProvider
import be.florien.anyflow.common.ui.list.SongViewHolder
import be.florien.anyflow.management.queue.model.QueueItemDisplay
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView


val diffCallback = object :
    DiffUtil.ItemCallback<QueueItemDisplay>() {
    override fun areItemsTheSame(oldItem: QueueItemDisplay, newItem: QueueItemDisplay) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: QueueItemDisplay, newItem: QueueItemDisplay): Boolean =
        oldItem.artist == newItem.artist
                && oldItem.album == newItem.album
                && oldItem.title == newItem.title

}

class QueueItemAdapter(
    val listener: SongListViewHolderListener,
    val provider: SongListViewHolderProvider,
    private val onSongClicked: (Int) -> Unit
) : PagingDataAdapter<QueueItemDisplay, SongViewHolder>(diffCallback),
    FastScrollRecyclerView.SectionedAdapter {

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