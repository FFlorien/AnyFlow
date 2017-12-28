package be.florien.ampacheplayer.manager

import android.app.Activity
import android.content.Intent
import be.florien.ampacheplayer.view.activity.PlayerActivity
import javax.inject.Inject

/**
 * Created by florien on 27/12/17.
 */
class Navigator(@Inject val activity: Activity) { //todo scope
    fun goToPlayer() {
        activity.startActivity(Intent(activity, PlayerActivity::class.java))
    }
}