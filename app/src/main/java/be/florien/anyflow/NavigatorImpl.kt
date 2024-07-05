package be.florien.anyflow

import android.content.Context
import be.florien.anyflow.common.ui.navigation.Navigator
import be.florien.anyflow.feature.player.ui.PlayerActivity
import be.florien.anyflow.utils.startActivity
import javax.inject.Inject

class NavigatorImpl @Inject constructor(): Navigator {
    override fun navigateToPlayer(context: Context) {
        context.startActivity(PlayerActivity::class)
    }
}