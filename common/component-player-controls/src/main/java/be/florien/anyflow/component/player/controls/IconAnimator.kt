package be.florien.anyflow.component.player.controls

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat


abstract class IconAnimator(private val context: Context) {
    var icon: Drawable? = null
    var onIconChanged: (() -> Unit)? = null
    protected var oldState: Int = -1
    protected var iconColor = ContextCompat.getColor(context, R.color.iconInApp)

    open fun computeIcon(newState: Int, iconPosition: Rect) {
        if (newState == oldState) {
            return
        }

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

    private fun assignIcon(
        newIcon: Drawable,
        playPausePosition: Rect,
        onAnimationEndAction: (() -> Unit)? = null
    ) {
        (icon as? AnimatedVectorDrawableCompat)?.clearAnimationCallbacks()
        icon = newIcon
        if (onAnimationEndAction != null && newIcon is AnimatedVectorDrawableCompat) {
            val callback = object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable) {
                    onAnimationEndAction()
                }
            }
            newIcon.registerAnimationCallback(callback)
        }

        newIcon.bounds = playPausePosition
        onIconChanged?.invoke()
        (newIcon as? AnimatedVectorDrawableCompat)?.start()
    }

    protected fun getIcon(animIconRes: Int): VectorDrawableCompat {
        val icon = VectorDrawableCompat.create(context.resources, animIconRes, context.theme)
            ?: throw IllegalArgumentException("Icon wasn't found !")
        icon.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(iconColor, BlendModeCompat.SRC_IN)
        return icon
    }

    protected fun getAnimatedIcon(animIconRes: Int): AnimatedVectorDrawableCompat {
        val icon = AnimatedVectorDrawableCompat.create(context, animIconRes)
            ?: throw IllegalArgumentException("Icon wasn't found !")
        icon.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(iconColor, BlendModeCompat.SRC_IN)
        return icon
    }

    abstract fun getStartAnimation(newState: Int): AnimatedVectorDrawableCompat?
    abstract fun getEndAnimation(newState: Int): AnimatedVectorDrawableCompat?
    abstract fun getFixedIcon(newState: Int): Drawable
}