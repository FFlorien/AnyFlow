package be.florien.anyflow.feature.playlist.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.common.di.ViewModelKey
import be.florien.anyflow.feature.playlist.list.PlaylistListViewModel
import be.florien.anyflow.feature.playlist.songs.PlaylistSongsViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class PlaylistViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(PlaylistListViewModel::class)
    abstract fun bindsPlaylistListVM(viewModel: PlaylistListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PlaylistSongsViewModel::class)
    abstract fun bindsPlaylistSongsViewModelVM(viewModel: PlaylistSongsViewModel): ViewModel
}