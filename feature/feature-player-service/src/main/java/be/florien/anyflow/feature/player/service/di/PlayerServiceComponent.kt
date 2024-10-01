package be.florien.anyflow.feature.player.service.di

import be.florien.anyflow.feature.player.service.PlayerService
import dagger.Subcomponent

@Subcomponent
interface PlayerServiceComponent {

    fun inject(service: PlayerService)

    @Subcomponent.Builder
    interface Builder {

        fun build(): PlayerServiceComponent
    }
}