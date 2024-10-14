package be.florien.anyflow.feature.song.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.architecture.di.ViewModelKey
import be.florien.anyflow.feature.song.ui.SongInfoViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class SongInfoViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(SongInfoViewModel::class)
    abstract fun bindsInfoDisplayFragmentVM(viewModel: SongInfoViewModel): ViewModel
}