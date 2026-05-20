package be.florien.anyflow.feature.library.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.common.di.ViewModelKey
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel
import be.florien.anyflow.feature.library.ui.list.LibraryListViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class LibraryViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(LibraryInfoViewModel::class)
    abstract fun bindsLibraryInfoVM(viewModel: LibraryInfoViewModel): ViewModel
}