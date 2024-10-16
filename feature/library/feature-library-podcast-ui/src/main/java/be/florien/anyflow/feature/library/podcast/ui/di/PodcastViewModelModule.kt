package be.florien.anyflow.feature.library.podcast.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.common.di.ViewModelKey
import be.florien.anyflow.feature.library.podcast.ui.info.LibraryPodcastInfoViewModel
import be.florien.anyflow.feature.library.podcast.ui.list.viewmodels.LibraryPodcastEpisodeListViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class PodcastViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(LibraryPodcastInfoViewModel::class)
    abstract fun bindsSelectFilterTypeViewModel(viewModel: LibraryPodcastInfoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LibraryPodcastEpisodeListViewModel::class)
    abstract fun bindsSelectFilterFragmentPodcastEpisodeVM(viewModel: LibraryPodcastEpisodeListViewModel): ViewModel
}