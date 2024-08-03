package be.florien.anyflow.injection

import be.florien.anyflow.architecture.di.ActivityScope
import be.florien.anyflow.feature.library.tags.ui.di.LibraryViewModelModule
import be.florien.anyflow.feature.player.ui.PlayerActivity
import dagger.Subcomponent

@Subcomponent(modules = [ViewModelModule::class, LibraryViewModelModule::class])
@ActivityScope
interface PlayerComponent {

    fun inject(playerActivity: PlayerActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): PlayerComponent
    }

}
