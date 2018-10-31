package be.florien.anyflow.view.menu

import android.view.Menu
import android.view.MenuInflater

class MenuCoordinator {
    private val menuHolders = mutableMapOf<Int, MenuHolder>()
    private val menuResourcesCount = mutableMapOf<Int, Int>()

    fun addMenuHolder(menuHolder: MenuHolder) {
        menuHolders[menuHolder.menuId] = menuHolder
        val currentCount = menuResourcesCount[menuHolder.menuResource] ?: 0
        menuResourcesCount[menuHolder.menuResource] = currentCount + 1
    }

    fun removeMenuHolder(menuHolder: MenuHolder) {
        menuHolders.remove(menuHolder.menuId)
        val currentCount = menuResourcesCount[menuHolder.menuResource] ?: 0
        if (currentCount > 1) {
            menuResourcesCount[menuHolder.menuResource] = currentCount - 1
        } else {
            menuResourcesCount.remove(menuHolder.menuResource)
        }
    }

    fun inflateMenus(menu: Menu, inflater: MenuInflater) {
        for (menuResource in menuResourcesCount.keys) {
            inflater.inflate(menuResource, menu)
        }
    }

    fun prepareMenus(menu: Menu) {
        for (creator in menuHolders.values) {
            creator.prepareMenu(menu)
        }
    }

    fun handleMenuClick(menuItemId: Int): Boolean {
        menuHolders[menuItemId]?.action?.invoke() ?: return false
        return true
    }
}