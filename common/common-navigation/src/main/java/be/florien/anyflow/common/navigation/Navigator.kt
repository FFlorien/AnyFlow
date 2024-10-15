package be.florien.anyflow.common.navigation

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import be.florien.anyflow.common.ui.data.TagType

interface Navigator {
    fun navigateToMain(context: Context, clearTop: Boolean = false)
    fun navigateToCurrentlyPlaying(context: Context)
    fun navigateToAlarm(context: Context)
    fun navigateToPlaylist(context: Context)
    fun navigateToShortcut(context: Context)

    fun displayFragmentOnMain(
        context: Context,
        fragment: Fragment,
        backstackName: String?,
        tag: String
    )

    fun displayPlaylistSelection(
        fragmentManager: FragmentManager,
        id: Long,
        type: TagType,
        secondId: Int
    )
}