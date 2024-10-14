package be.florien.anyflow.feature.playlist.di

import be.florien.anyflow.architecture.di.ActivityScope
import be.florien.anyflow.feature.playlist.list.PlaylistListViewModel
import be.florien.anyflow.feature.playlist.songs.PlaylistSongsFragment
import be.florien.anyflow.feature.playlist.songs.PlaylistSongsViewModel
import dagger.Subcomponent

@Subcomponent(modules = [PlaylistViewModelModule::class])
@ActivityScope
interface PlaylistActivityComponent {
    fun inject(playlistSongsFragment: PlaylistSongsFragment)
    fun inject(viewModel: PlaylistSongsViewModel)
    fun inject(viewModel: PlaylistListViewModel)

    @Subcomponent.Builder
    interface Builder {

        fun build(): PlaylistActivityComponent
    }

}
