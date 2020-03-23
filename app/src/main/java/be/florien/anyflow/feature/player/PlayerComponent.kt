package be.florien.anyflow.feature.player

import android.app.Activity
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.injection.ViewModelModule
import dagger.BindsInstance
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
