package be.florien.anyflow.injection

import androidx.lifecycle.ViewModel
import be.florien.anyflow.MainActivityViewModel
import be.florien.anyflow.common.di.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class MainActivityViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(MainActivityViewModel::class)
    abstract fun bindsPlayerActivityVM(viewModel: MainActivityViewModel): ViewModel
}