package be.florien.anyflow.feature.podcast.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.common.di.ViewModelKey
import be.florien.anyflow.feature.podcast.ui.PodcastInfoViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class PodcastInfoViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(PodcastInfoViewModel::class)
    abstract fun bindsInfoDisplayFragmentVM(viewModel: PodcastInfoViewModel): ViewModel
}