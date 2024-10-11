package be.florien.anyflow.common.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

interface MainScreen {
    val containerId: Int
    val mainScreenFragmentManager: FragmentManager
}

interface MainScreenSection {
    val isFirstSection: Boolean
    val menuId: Int
    val tag: String

    fun createFragment(): Fragment
}