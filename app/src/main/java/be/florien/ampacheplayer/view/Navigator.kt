package be.florien.ampacheplayer.view

import android.app.Activity
import be.florien.ampacheplayer.extension.startActivity
import be.florien.ampacheplayer.view.connect.ConnectActivity
import be.florien.ampacheplayer.view.player.PlayerActivity
import javax.inject.Inject

/**
 * Created by florien on 27/12/17.
 */
class Navigator(@set:Inject var activity: Activity) { //todo scope
    fun goToPlayer() {
        activity.startActivity(PlayerActivity::class)
    }

    fun goToConnection() {
        activity.startActivity(ConnectActivity::class)
    }
}