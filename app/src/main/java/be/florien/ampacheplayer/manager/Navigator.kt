package be.florien.ampacheplayer.manager

import android.app.Activity
import be.florien.ampacheplayer.extension.startActivity
import be.florien.ampacheplayer.view.activity.ConnectActivity
import be.florien.ampacheplayer.view.activity.PlayerActivity
import javax.inject.Inject

/**
 * Created by florien on 27/12/17.
 */
class Navigator(@Inject private val activity: Activity) { //todo scope
    fun goToPlayer() {
        activity.startActivity(PlayerActivity::class)
    }

    fun goToConnection() {
        activity.startActivity(ConnectActivity::class)
    }
}