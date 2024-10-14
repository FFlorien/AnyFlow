package be.florien.anyflow.feature.player.ui

import androidx.fragment.app.Fragment

interface MainScreenSection {
    val isFirstSection: Boolean
    val menuId: Int
    val tag: String

    fun createFragment(): Fragment
}