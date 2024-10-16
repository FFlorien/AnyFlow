package be.florien.anyflow.feature.player.ui.di

import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.feature.player.ui.MainActivity
import dagger.Subcomponent

@Subcomponent(modules = [MainActivityViewModelModule::class])
@ActivityScope
interface PlayerActivityComponent {

    fun inject(mainActivity: MainActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): PlayerActivityComponent
    }

}
