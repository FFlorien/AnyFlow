package be.florien.anyflow.feature.player.details

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.absoluteValue

abstract class DetailViewHolder<T, B : ViewDataBinding, L : DetailViewHolderListener<T>>(
    parent: ViewGroup,
    val listener: L,
    val binding: B
) : RecyclerView.ViewHolder(binding.root) {

    protected var startingTranslationX: Float = 0f
    abstract val itemInfoView: View
    abstract val infoView: View
    abstract val item: T?

    init {
        setClickListener(parent is RecyclerView)
    }

    abstract fun bind(item: T?)

    fun openInfoWhenSwiped() {
        if (
            itemView.visibility == View.VISIBLE
            && itemInfoView.translationX > infoView.right - 10
            && startingTranslationX == 0f
        ) {
            val song = item ?: return
            listener.onInfoDisplayAsked(song)
        }
    }

    open fun swipeForMove(translateX: Float): Boolean {
        if (translateX > 0F) {
            val translationToSeeInfo = (infoView.right).toFloat()
            val translationToFollowMove = startingTranslationX + translateX
            itemInfoView.translationX =
                minOf(translationToSeeInfo, translationToFollowMove)
                    .coerceAtLeast(startingTranslationX)
            return true
        }

        return false
    }

    private fun setClickListener(isRecyclerView: Boolean) {
        if (isRecyclerView) {
            itemInfoView.setOnClickListener {
                itemInfoView.translationX = 0F
                listener.onItemClick(absoluteAdapterPosition)
            }
        }
        itemInfoView.setOnLongClickListener {
            swipeForInfo()
            return@setOnLongClickListener true
        }
    }

    private fun swipeForInfo() {
        ObjectAnimator.ofFloat(
            itemInfoView,
            View.TRANSLATION_X,
            infoView.right.toFloat()
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
                    val nullSafeItem = item ?: return
                    listener.onInfoDisplayAsked(nullSafeItem)
                }
            })
            start()
        }
    }
}

abstract class ItemInfoTouchAdapter {
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
                    viewHolder.swipeToOpenQuickActions()
                } else {
                    viewHolder.openInfoWhenSwiped()
                    viewHolder.swipeToCloseQuickActions()
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