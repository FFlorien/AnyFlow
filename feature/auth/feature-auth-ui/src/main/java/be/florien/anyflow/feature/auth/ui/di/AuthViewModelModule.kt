package be.florien.anyflow.feature.auth.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.architecture.di.ViewModelKey
import be.florien.anyflow.feature.auth.ui.server.ServerViewModel
import be.florien.anyflow.feature.auth.ui.user.UserConnectViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class AuthViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(ServerViewModel::class)
    abstract fun bindsServerViewModel(viewModel: ServerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserConnectViewModel::class)
    abstract fun bindsUserConnectViewModel(viewModel: UserConnectViewModel): ViewModel

}