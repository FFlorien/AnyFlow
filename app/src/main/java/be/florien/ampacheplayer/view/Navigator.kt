package be.florien.ampacheplayer.view

import android.app.Activity
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.extension.startActivity
import be.florien.ampacheplayer.view.connect.ConnectActivityBase
import be.florien.ampacheplayer.view.player.PlayerActivity
import javax.inject.Inject

/**
 * Created by florien on 27/12/17.
 */
@ActivityScope
class Navigator @Inject constructor(var activity: Activity) {
    fun goToPlayer() {
        activity.startActivity(PlayerActivity::class)
    }

    fun goToConnection() {
        activity.startActivity(ConnectActivityBase::class)
    }
}