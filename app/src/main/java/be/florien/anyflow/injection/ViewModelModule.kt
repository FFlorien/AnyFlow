package be.florien.anyflow.injection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.feature.player.ui.PlayerViewModel
import be.florien.anyflow.feature.player.ui.filters.CurrentFilterViewModel
import be.florien.anyflow.feature.player.ui.info.song.SongInfoViewModel
import be.florien.anyflow.feature.player.ui.info.song.shortcuts.ShortcutsViewModel
import be.florien.anyflow.feature.player.ui.library.info.LibraryInfoViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryAlbumArtistListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryAlbumListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryArtistListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryDownloadedListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryGenreListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryPlaylistListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryPodcastEpisodeListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibrarySongListViewModel
import be.florien.anyflow.feature.player.ui.library.saved.SavedFilterGroupViewModel
import be.florien.anyflow.feature.player.ui.songlist.SongListViewModel
import be.florien.anyflow.feature.playlist.selection.SelectPlaylistViewModel
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(PlayerViewModel::class)
    abstract fun bindsPlayerActivityVM(viewModel: PlayerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SongListViewModel::class)
    abstract fun bindsSongListFragmentVM(viewModel: SongListViewModel): ViewModel

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

    @Binds
    @IntoMap
    @ViewModelKey(SavedFilterGroupViewModel::class)
    abstract fun bindsSavedFilterGroupVM(viewModel: SavedFilterGroupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CurrentFilterViewModel::class)
    abstract fun bindsDisplayFilterFragmentVM(viewModel: CurrentFilterViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SongInfoViewModel::class)
    abstract fun bindsInfoDisplayFragmentVM(viewModel: SongInfoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ShortcutsViewModel::class)
    abstract fun bindsInfoActionsSelectionViewModel(viewModel: ShortcutsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SelectPlaylistViewModel::class)
    abstract fun bindsSelectPlaylistFragmentVM(viewModel: SelectPlaylistViewModel): ViewModel

    @Binds
    abstract fun bindsViewModelFactory(factory: AnyFlowViewModelFactory): ViewModelProvider.Factory
}

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ViewModelKey(val value: KClass<out ViewModel>)