package be.florien.anyflow.feature.auth.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.common.di.ViewModelKey
import be.florien.anyflow.feature.auth.ui.user.UserConnectViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class AuthViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(UserConnectViewModel::class)
    abstract fun bindsUserConnectViewModel(viewModel: UserConnectViewModel): ViewModel

}