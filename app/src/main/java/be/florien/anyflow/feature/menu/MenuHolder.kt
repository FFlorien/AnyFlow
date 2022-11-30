package be.florien.anyflow.feature.menu

import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import android.view.Menu
import android.view.MenuItem

abstract class MenuHolder(
    @get:MenuRes
    val menuResource: Int,
    @get:IdRes
    val menuId: Int,
    val action: () -> Unit
) {

    protected var menuItem: MenuItem? = null
    var isVisible = true
        set(value) {
            field = value
            menuItem?.isVisible = value
        }
    var isEnabled = true
        set(value) {
            field = value
            menuItem?.isEnabled = value
            menuItem?.icon?.alpha = if (value) {
                0xFF
            } else {
                0x80
            }
        }

    open fun prepareMenu(menu: Menu) {
        menuItem = menu.findItem(menuId)
        menuItem?.isVisible = isVisible
    }
}