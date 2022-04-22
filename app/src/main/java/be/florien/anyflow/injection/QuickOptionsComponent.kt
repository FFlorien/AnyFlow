package be.florien.anyflow.injection

import be.florien.anyflow.feature.quickOptions.QuickOptionsActivity
import dagger.Subcomponent

@Subcomponent(modules = [ViewModelModule::class])
@ActivityScope
interface QuickOptionsComponent {

    fun inject(quickOptionsActivity: QuickOptionsActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): QuickOptionsComponent
    }

}
