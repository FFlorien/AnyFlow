package be.florien.anyflow.feature.auth.ui.di

import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.feature.auth.ui.user.AuthenticationViewModel
import dagger.Subcomponent

@Subcomponent()
@ActivityScope
interface AuthenticationViewModelComponent {
    fun inject(authenticationActivity: AuthenticationViewModel)

    @Subcomponent.Builder
    interface Builder {

        fun build(): AuthenticationViewModelComponent
    }

}
