package be.florien.anyflow.feature.player.ui.di

import be.florien.anyflow.architecture.di.ActivityScope
import be.florien.anyflow.feature.player.ui.PlayerActivity
import dagger.Subcomponent

@Subcomponent
@ActivityScope
interface PlayerActivityComponent {

    fun inject(playerActivity: PlayerActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): PlayerActivityComponent
    }

}
