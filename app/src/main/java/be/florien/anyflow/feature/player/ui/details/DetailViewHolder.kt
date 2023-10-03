package be.florien.anyflow.feature.player.ui.details

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.absoluteValue

abstract class DetailViewHolder<T>(val listener: DetailViewHolderListener<T>, view: View) :
    RecyclerView.ViewHolder(view) {

    protected var startingTranslationX: Float = 0f

    abstract val itemInfoView: View
    abstract val infoIconView: View
    abstract val item: T?

    open fun swipeForMove(translateX: Float): Boolean {
        if (translateX > 0F) {
            val translationToSeeInfo = (infoIconView.right).toFloat()
            val translationToFollowMove = startingTranslationX + translateX
            itemInfoView.translationX =
                minOf(translationToSeeInfo, translationToFollowMove)
                    .coerceAtLeast(startingTranslationX)
            return true
        }

        return false
    }

    open fun swipeToClose() {
        ObjectAnimator.ofFloat(itemInfoView, View.TRANSLATION_X, 0f).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()
            start()
        }
        startingTranslationX = 0F
    }

    fun openInfoWhenSwiped(): Boolean {
        return if (
            isSwipedEnoughForInfo()
            && startingTranslationX == 0f
        ) {
            val infoItem = item ?: return false
            listener.onInfoDisplayAsked(infoItem)
            swipeToClose()
            true
        } else {
            false
        }
    }

    protected fun setClickListener() {
        itemInfoView.setOnLongClickListener {
            swipeForInfo()
            return@setOnLongClickListener true
        }
    }

    private fun isSwipedEnoughForInfo(): Boolean =
        itemInfoView.translationX > infoIconView.right - 10

    private fun swipeForInfo() {
        ObjectAnimator.ofFloat(
            itemInfoView,
            View.TRANSLATION_X,
            infoIconView.right.toFloat()
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
    protected var hasSwiped: Boolean = false

    protected open fun onTouch(viewHolder: DetailViewHolder<*>, event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                viewHolder.swipeForMove(event.x - downTouchX)
                true
            }

            MotionEvent.ACTION_UP -> {
                viewHolder.openInfoWhenSwiped()
            }

            else -> false
        }
    }

    protected fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downTouchX = e.x
                downTouchY = e.y
                hasSwiped = false
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = downTouchX - e.x
                val deltaY = downTouchY - e.y
                hasSwiped = hasSwiped || deltaX.absoluteValue > deltaY.absoluteValue
                return hasSwiped
            }

            MotionEvent.ACTION_UP -> {
                val stopSwipe = hasSwiped
                downTouchX = -1f
                downTouchY = -1f
                return stopSwipe
            }
        }
        return false
    }
}

interface DetailViewHolderListener<T> {
    fun onInfoDisplayAsked(item: T)
}