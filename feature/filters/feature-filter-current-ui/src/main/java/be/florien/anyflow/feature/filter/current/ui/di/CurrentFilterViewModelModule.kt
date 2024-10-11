package be.florien.anyflow.feature.filter.current.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.architecture.di.ViewModelKey
import be.florien.anyflow.feature.filter.current.ui.CurrentFilterViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class CurrentFilterViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(CurrentFilterViewModel::class)
    abstract fun bindsCurrentFilterViewModel(viewModel: CurrentFilterViewModel): ViewModel
}