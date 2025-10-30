package be.florien.anyflow.feature.auth.ui.di

import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.feature.auth.ui.user.AuthenticationActivity
import dagger.Subcomponent

@Subcomponent(modules = [AuthViewModelModule::class])
@ActivityScope
interface AuthenticationActivityComponent {
    fun inject(authenticationActivity: AuthenticationActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): AuthenticationActivityComponent
    }

}
