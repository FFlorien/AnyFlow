package be.florien.anyflow.feature.library.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.architecture.di.ViewModelKey
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryAlbumArtistListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryAlbumListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryArtistListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryDownloadedListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryGenreListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryPlaylistListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryPodcastEpisodeListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibrarySongListViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class LibraryViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(LibraryInfoViewModel::class)
    abstract fun bindsSelectFilterTypeViewModel(viewModel: LibraryInfoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LibraryAlbumListViewModel::class)
    abstract fun bindsSelectFilterFragmentAlbumVM(viewModel: LibraryAlbumListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LibraryAlbumArtistListViewModel::class)
    abstract fun bindsSelectFilterFragmentAlbumArtistVM(viewModel: LibraryAlbumArtistListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LibraryArtistListViewModel::class)
    abstract fun bindsSelectFilterFragmentArtistVM(viewModel: LibraryArtistListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LibraryGenreListViewModel::class)
    abstract fun bindsSelectFilterFragmentGenreVM(viewModel: LibraryGenreListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LibrarySongListViewModel::class)
    abstract fun bindsSelectFilterFragmentSongVM(viewModel: LibrarySongListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LibraryPlaylistListViewModel::class)
    abstract fun bindsSelectFilterFragmentPlaylistVM(viewModel: LibraryPlaylistListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LibraryDownloadedListViewModel::class)
    abstract fun bindsSelectFilterFragmentDownloadedVM(viewModel: LibraryDownloadedListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LibraryPodcastEpisodeListViewModel::class)
    abstract fun bindsSelectFilterFragmentPodcastEpisodeVM(viewModel: LibraryPodcastEpisodeListViewModel): ViewModel
}