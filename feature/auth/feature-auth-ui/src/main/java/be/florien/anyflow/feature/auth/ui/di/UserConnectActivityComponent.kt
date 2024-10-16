package be.florien.anyflow.feature.auth.ui.di

import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.feature.auth.ui.user.UserConnectActivityBase
import dagger.Subcomponent

@Subcomponent(modules = [AuthViewModelModule::class])
@ActivityScope
interface UserConnectActivityComponent {
    fun inject(userConnectActivityBase: UserConnectActivityBase)

    @Subcomponent.Builder
    interface Builder {

        fun build(): UserConnectActivityComponent
    }

}
