package be.florien.anyflow.feature.mediaList.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.common.di.ViewModelKey
import be.florien.anyflow.feature.mediaList.ui.MediaListViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class MediaListViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(MediaListViewModel::class)
    abstract fun bindsMediaListFragmentVM(viewModel: MediaListViewModel): ViewModel
}