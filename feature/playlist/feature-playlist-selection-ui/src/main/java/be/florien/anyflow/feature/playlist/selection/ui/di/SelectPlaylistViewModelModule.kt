package be.florien.anyflow.feature.playlist.selection.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.architecture.di.ViewModelKey
import be.florien.anyflow.feature.playlist.selection.ui.SelectPlaylistViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class SelectPlaylistViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(SelectPlaylistViewModel::class)
    abstract fun bindsPlaylistListVM(viewModel: SelectPlaylistViewModel): ViewModel
}