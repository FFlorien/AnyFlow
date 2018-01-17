package be.florien.ampacheplayer

import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.ActivityComponent

/**
 * Created by FlamentF on 17-Jan-18.
 */
interface IApplicationComponent {

    fun activityComponentBuilder(): ActivityComponent.Builder

    fun inject(playerService: PlayerService)

}