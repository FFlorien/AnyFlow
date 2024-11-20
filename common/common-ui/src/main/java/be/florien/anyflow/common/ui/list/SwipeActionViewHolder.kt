package be.florien.anyflow.common.ui.list

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.absoluteValue

abstract class SwipeActionViewHolder(
    view: View,
    val topView: View,
    private val hiddenView: View
) :
    RecyclerView.ViewHolder(view) {

    protected var startingTranslationX: Float = 0f

    abstract fun swipeAction()

    init {
        setLongClickListener()
    }

    open fun moveToSwipe(translateX: Float): Boolean {
        if (translateX > 0F) {
            val translationToSeeInfo = (hiddenView.right).toFloat()
            val translationToFollowMove = startingTranslationX + translateX
            topView.translationX =
                minOf(translationToSeeInfo, translationToFollowMove)
                    .coerceAtLeast(startingTranslationX)
            return true
        }

        return false
    }

    open fun resetSwipePosition() {
        ObjectAnimator.ofFloat(topView, View.TRANSLATION_X, 0f).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()
            start()
        }
        startingTranslationX = 0F
    }

    fun triggerSwipeAction(): Boolean {
        return if (
            isSwipedEnoughForAction()
            && startingTranslationX == 0f
        ) {
            swipeAction()
            resetSwipePosition()
            true
        } else {
            false
        }
    }

    protected fun setLongClickListener() {
        topView.setOnLongClickListener {
            swipeForAction()
            return@setOnLongClickListener true
        }
    }

    private fun isSwipedEnoughForAction(): Boolean =
        topView.translationX > hiddenView.right - 10

    private fun swipeForAction() {
        ObjectAnimator.ofFloat(
            topView,
            View.TRANSLATION_X,
            hiddenView.right.toFloat()
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
                    swipeAction()
                }
            })
            start()
        }
    }
}

abstract class ItemInfoTouchAdapter {
    var downTouchX: Float = -1f
    var downTouchY: Float = -1f
    protected var hasSwiped: Boolean = false

    protected open fun onTouch(viewHolder: SwipeActionViewHolder, event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                viewHolder.moveToSwipe(event.x - downTouchX)
                true
            }

            MotionEvent.ACTION_UP -> {
                viewHolder.triggerSwipeAction()
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