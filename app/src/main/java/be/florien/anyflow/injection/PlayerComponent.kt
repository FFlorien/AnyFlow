package be.florien.anyflow.injection

import be.florien.anyflow.feature.player.ui.PlayerActivity
import dagger.Subcomponent

@Subcomponent(modules = [ViewModelModule::class])
@ActivityScope
interface PlayerComponent {

    fun inject(playerActivity: PlayerActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): PlayerComponent
    }

}
