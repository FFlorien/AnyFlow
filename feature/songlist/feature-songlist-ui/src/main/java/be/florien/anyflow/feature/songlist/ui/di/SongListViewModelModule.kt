package be.florien.anyflow.feature.songlist.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.architecture.di.ViewModelKey
import be.florien.anyflow.feature.songlist.ui.SongListViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class SongListViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(SongListViewModel::class)
    abstract fun bindsSongListFragmentVM(viewModel: SongListViewModel): ViewModel
}