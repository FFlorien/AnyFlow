package be.florien.anyflow.feature.filter.saved.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.common.di.ViewModelKey
import be.florien.anyflow.feature.filter.saved.ui.SavedFilterGroupViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class SavedFilterGroupViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(SavedFilterGroupViewModel::class)
    abstract fun bindsSavedFilterGroupViewModel(viewModel: SavedFilterGroupViewModel): ViewModel
}