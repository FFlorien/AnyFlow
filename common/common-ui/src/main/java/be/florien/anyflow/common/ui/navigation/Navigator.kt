package be.florien.anyflow.common.ui.navigation

import android.content.Context
import androidx.fragment.app.Fragment

interface Navigator {

    fun navigateToPlayer(context: Context, clearTop: Boolean = false)
    fun navigateToCurrentlyPlaying(context: Context)
    fun displayFragmentOnMain(context: Context, fragment: Fragment, tag: String)
}