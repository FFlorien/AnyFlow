package be.florien.anyflow

import android.content.Context
import androidx.fragment.app.Fragment
import be.florien.anyflow.common.ui.navigation.Navigator
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.player.ui.PlayerActivity
import be.florien.anyflow.utils.startActivity
import javax.inject.Inject

class NavigatorImpl @Inject constructor(): Navigator {
    override fun navigateToPlayer(context: Context) {
        context.startActivity(PlayerActivity::class)
    }

    override fun navigateToCurrentlyPlaying(context: Context) {
        (context as PlayerActivity).displaySongList()
    }

    override fun displayFragmentOnMain(context: Context, fragment: Fragment, tag: String) {
        (context as PlayerActivity).supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.container,
                fragment,
                tag
            )
            .addToBackStack(null)
            .commit()
    }
}