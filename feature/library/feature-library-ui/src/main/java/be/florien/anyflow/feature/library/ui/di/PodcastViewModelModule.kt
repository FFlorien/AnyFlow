package be.florien.anyflow.feature.library.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.common.di.ViewModelKey
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryPodcastEpisodeListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryPodcastListViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class PodcastViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(LibraryPodcastEpisodeListViewModel::class)
    abstract fun bindsSelectFilterFragmentPodcastEpisodeVM(viewModel: LibraryPodcastEpisodeListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LibraryPodcastListViewModel::class)
    abstract fun bindsSelectFilterFragmentPodcastVM(viewModel: LibraryPodcastListViewModel): ViewModel
}