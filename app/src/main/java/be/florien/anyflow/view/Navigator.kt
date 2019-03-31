package be.florien.anyflow.view

import android.app.Activity
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.extension.startActivity
import be.florien.anyflow.view.player.PlayerActivity
import javax.inject.Inject

@ActivityScope
class Navigator @Inject constructor(var activity: Activity) {
    fun goToPlayer() {
        activity.startActivity(PlayerActivity::class)
        activity.finish()
    }
}