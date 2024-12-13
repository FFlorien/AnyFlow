package be.florien.anyflow.component.viewholder

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.feature.songlist.base.domain.model.QueueItemInfoRow
import kotlin.math.absoluteValue


abstract class ShortcutsViewHolder(
    view: View,
    topView: View,
    hiddenView: View,
    protected val shortcutsContainer: ViewGroup,
    private val getShortcuts: () -> List<QueueItemInfoRow<*, *>>
) : SwipeActionViewHolder(view, topView, hiddenView) {

    abstract fun View.bindShortcut(action: QueueItemInfoRow<*, *>)
    abstract fun performDefaultShortcut()
    abstract fun onShortcutOpened(position: Int?)

    private val lastEmptyShortcutView: View? = shortcutsContainer.children.lastOrNull()
    private val emptyShortcutViewCount = shortcutsContainer.childCount
    private val shortcutStart: Int
        get() = lastEmptyShortcutView?.right ?: shortcutsContainer.left

    init {
        setShortcuts()
    }

    /**
     * Overridden methods
     */

    override fun moveToSwipe(translateX: Float): Boolean {
        if (!super.moveToSwipe(translateX) && getShortcuts().isNotEmpty()) {
            val translationToSeeActions = (shortcutStart - itemView.width).toFloat()
            val translationToFollowMove = startingTranslationX + translateX
            topView.translationX =
                maxOf(translationToSeeActions, translationToFollowMove).coerceAtMost(0f)
            return true
        }
        return false
    }

    /**
     * Public methods
     */

    fun setShortcuts() {
        val childCount = shortcutsContainer.childCount
        if (childCount > emptyShortcutViewCount) {
            shortcutsContainer.removeViews(
                emptyShortcutViewCount,
                childCount - emptyShortcutViewCount
            )
        }
        val newActions = getShortcuts().reversed()
        for (action in newActions) {
            val itemAction = if (newActions.size == 1) {
                R.layout.item_action_unique
            } else {
                R.layout.item_action
            }
            shortcutsContainer.addView(
                (LayoutInflater.from(itemView.context)
                    .inflate(itemAction, shortcutsContainer, false))
                    .apply {
                        bindShortcut(action)
                    })
        }
    }

    fun openShortcutWhenSwiped(): Boolean {
        val shortcutsWidth = shortcutStart - shortcutsContainer.right
        return if (topView.translationX < shortcutsWidth + (shortcutsWidth.absoluteValue / 4)) {
            if (shortcutsContainer.childCount == 3) {
                performDefaultShortcut()
                resetSwipePosition()
            } else {
                onShortcutOpened(absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION })
                val translationXEnd = shortcutStart - itemView.width.toFloat()
                ObjectAnimator
                    .ofFloat(
                        topView,
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

    protected fun openShortcuts() {
        val translationXEnd = shortcutStart - itemView.width.toFloat()
        topView.translationX = translationXEnd
        startingTranslationX = translationXEnd
    }
}