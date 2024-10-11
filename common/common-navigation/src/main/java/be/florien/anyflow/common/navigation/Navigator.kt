package be.florien.anyflow.common.navigation

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import be.florien.anyflow.common.ui.TagType

interface Navigator {

    fun navigateToConnect(context: Context)
    fun navigateToPlayer(context: Context, clearTop: Boolean = false)
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