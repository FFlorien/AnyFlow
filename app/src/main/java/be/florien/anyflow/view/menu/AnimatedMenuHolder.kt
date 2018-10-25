package be.florien.anyflow.view.menu

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.support.annotation.IdRes
import android.support.annotation.MenuRes
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
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