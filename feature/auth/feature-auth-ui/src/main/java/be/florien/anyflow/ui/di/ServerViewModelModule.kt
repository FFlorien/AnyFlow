package be.florien.anyflow.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.architecture.di.ViewModelKey
import be.florien.anyflow.ui.server.ServerViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ServerViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(ServerViewModel::class)
    abstract fun bindsServerViewModel(viewModel: ServerViewModel): ViewModel

}