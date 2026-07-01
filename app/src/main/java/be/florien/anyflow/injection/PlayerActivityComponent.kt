package be.florien.anyflow.injection

import be.florien.anyflow.MainActivity
import be.florien.anyflow.common.di.ActivityScope
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
