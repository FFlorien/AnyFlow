package be.florien.anyflow.component.viewholder

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.children
import be.florien.anyflow.feature.songlist.base.domain.model.QueueItemInfoRow
import be.florien.anyflow.management.queue.model.QueueItemDisplay

abstract class QueueItemViewHolder<T : QueueItemDisplay>(
    private val listener: QueueItemViewHolderListener,
    internal val provider: QueueItemViewHolderProvider,
    private val shouldAlwaysShowShortcuts: Boolean = false,
    view: View,
    topView: View,
    hiddenView: View,
    shortcutsContainer: ViewGroup
) : ShortcutsViewHolder(
    view,
    topView,
    hiddenView,
    shortcutsContainer,
    provider::getShortcuts
) {

    abstract val item: T?
    var isCurrent: Boolean = false

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

    abstract fun bind(queueItem: T?)

    /**
     * Overridden methods
     */

    override fun swipeAction() {
        item?.let {
            listener.onInfoDisplayAsked(it)
        }
    }

    override fun View.bindShortcut(action: QueueItemInfoRow<*, *>) {
        findViewById<ImageView>(R.id.action).setImageResource(action.actionType.iconRes)
        findViewById<ImageView>(R.id.field).setImageResource(action.fieldType.iconRes)
        setOnClickListener {
            val queueItem = item
            if (queueItem != null)
                listener.onShortcut(queueItem, action)
            if (!shouldAlwaysShowShortcuts)
                resetSwipePosition()
        }
    }

    override fun performDefaultShortcut() {
        if (item != null) {
            shortcutsContainer.children.last().performClick()
        }
    }

    override fun onShortcutOpened(position: Int?) {
        listener.onShortcutOpened(position)
    }
}

interface QueueItemViewHolderListener {
    fun onItemClick(position: Int)
    fun onShortcut(item: QueueItemDisplay, row: QueueItemInfoRow<*, *>)
    fun onCurrentShortcutsClosed()
    fun onInfoDisplayAsked(item: QueueItemDisplay)
    fun onShortcutOpened(position: Int?)
}

interface QueueItemViewHolderProvider {
    fun getCurrentPositionFor(): Int
    fun getCurrentTranslationX(): Float
    fun getArtUrl(item: QueueItemDisplay): String
    fun getShortcuts(): List<QueueItemInfoRow<*, *>>
}