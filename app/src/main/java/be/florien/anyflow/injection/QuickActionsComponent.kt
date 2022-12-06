package be.florien.anyflow.injection

import be.florien.anyflow.feature.player.info.song.quickActions.QuickActionsActivity
import dagger.Subcomponent

@Subcomponent(modules = [ViewModelModule::class])
@ActivityScope
interface QuickActionsComponent {

    fun inject(quickActionsActivity: QuickActionsActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): QuickActionsComponent
    }

}
