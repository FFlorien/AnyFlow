package be.florien.anyflow.view.customView

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import be.florien.anyflow.R


abstract class IconAnimator(val context: Context) {
    lateinit var icon: Drawable
    protected var oldState: Int = -1
    private var oldCallback: Animatable2Compat.AnimationCallback? = null
    protected var iconColor = ContextCompat.getColor(context, R.color.iconInApp)

    open fun computeIcon(newState: Int, iconPosition: Rect) {
        val startIcon = getStartAnimation(newState)
        val endIcon = getEndAnimation(newState)
        val fixedIcon = getFixedIcon(newState)

        when {
            startIcon != null -> {
                assignIcon(startIcon, iconPosition) {
                    if (endIcon != null) {
                        assignIcon(endIcon, iconPosition) {
                            assignIcon(fixedIcon, iconPosition)
                        }
                    } else {
                        assignIcon(fixedIcon, iconPosition)
                    }
                }
            }
            endIcon != null -> {
                assignIcon(endIcon, iconPosition) {
                    assignIcon(fixedIcon, iconPosition)
                }
            }
            else -> {
                assignIcon(fixedIcon, iconPosition)
            }
        }
        oldState = newState
    }

    private fun assignIcon(newIcon: Drawable, playPausePosition: Rect, onAnimationEndAction: (() -> Unit)? = null) {
        val nullSafeCallback = oldCallback
        if (nullSafeCallback != null) {
            (icon as? AnimatedVectorDrawableCompat)?.unregisterAnimationCallback(nullSafeCallback)
        }
        if (onAnimationEndAction != null && newIcon is AnimatedVectorDrawableCompat) {
            val callback = object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable) {
                    onAnimationEndAction()
                }
            }
            oldCallback = callback
            newIcon.registerAnimationCallback(callback)
        } else {
            oldCallback = null
        }
        icon = newIcon
        icon.bounds = playPausePosition
        (icon as? AnimatedVectorDrawableCompat)?.start()
    }

    protected fun getIcon(animIconRes: Int): VectorDrawableCompat {
        val icon = VectorDrawableCompat.create(context.resources, animIconRes, context.theme)
                ?: throw IllegalArgumentException("Icon wasn't found !")
        icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        return icon
    }

    protected fun getAnimatedIcon(animIconRes: Int): AnimatedVectorDrawableCompat {
        val icon = AnimatedVectorDrawableCompat.create(context, animIconRes)
                ?: throw IllegalArgumentException("Icon wasn't found !")
        icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        return icon
    }

    abstract fun getStartAnimation(newState: Int): AnimatedVectorDrawableCompat?
    abstract fun getEndAnimation(newState: Int): AnimatedVectorDrawableCompat?
    abstract fun getFixedIcon(newState: Int): Drawable
}