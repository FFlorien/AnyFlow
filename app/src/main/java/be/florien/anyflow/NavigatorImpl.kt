package be.florien.anyflow

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import be.florien.anyflow.common.ui.navigation.Navigator
import be.florien.anyflow.feature.auth.UserConnectActivity
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.player.ui.PlayerActivity
import javax.inject.Inject

class NavigatorImpl @Inject constructor() : Navigator {
    override fun navigateToConnect(context: Context) {
        context.startActivity(Intent(context, UserConnectActivity::class.java))
    }

    override fun navigateToPlayer(context: Context, clearTop: Boolean) {
        val intent = Intent(context, PlayerActivity::class.java)
        if (clearTop) {
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    override fun navigateToCurrentlyPlaying(context: Context) {
        (context as PlayerActivity).displaySongList()
    }

    override fun displayFragmentOnMain(
        context: Context,
        fragment: Fragment,
        backstackName: String?,
        tag: String
    ) {
        (context as PlayerActivity)
            .supportFragmentManager
            .beginTransaction()
            .apply {
                replace(R.id.container, fragment, tag)
                addToBackStack(backstackName)
            }
            .commit()
    }
}