package be.florien.anyflow.feature.menu

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import android.view.Menu

abstract class AnimatedMenuHolder(
        @MenuRes
        menuResource: Int,
        @IdRes
        menuId: Int,
        @DrawableRes
        val firstStateDrawableResource: Int,
        @DrawableRes
        val secondStateDrawableResource: Int,
        var isIconInFirstState: Boolean,
        val context: Context,
        action: () -> Unit) : MenuHolder(menuResource, menuId, action) {

    private val firstStateDrawable: AnimatedVectorDrawableCompat
        get() = AnimatedVectorDrawableCompat.create(context, firstStateDrawableResource)?.apply {
            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    firstStateDrawable
                    menuItem?.icon = secondStateDrawable
                    isIconInFirstState = false
                }
            })
        } ?: throw IllegalStateException("Error parsing the vector drawable")
    private val secondStateDrawable: AnimatedVectorDrawableCompat
        get() = AnimatedVectorDrawableCompat.create(context, secondStateDrawableResource)?.apply {
            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    menuItem?.icon = firstStateDrawable
                    isIconInFirstState = true
                }
            })
        } ?: throw IllegalStateException("Error parsing the vector drawable")

    override fun prepareMenu(menu: Menu) {
        super.prepareMenu(menu)
        setState(isIconInFirstState)
    }

    fun changeState(toFirstState: Boolean) {
        if (isIconInFirstState != toFirstState) {
            if (isVisible && menuItem != null) {
                (menuItem?.icon as? Animatable)?.start()
            } else {
                setState(toFirstState)
            }
        }
    }

    private fun setState(isFirstState: Boolean) {
        isIconInFirstState = isFirstState
        menuItem?.icon = if (isFirstState) firstStateDrawable else secondStateDrawable
    }

}