package be.florien.anyflow.injection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.feature.player.PlayerViewModel
import be.florien.anyflow.feature.player.filter.display.DisplayFilterViewModel
import be.florien.anyflow.feature.player.filter.saved.SavedFilterGroupViewModel
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel
import be.florien.anyflow.feature.player.filter.selection.*
import be.florien.anyflow.feature.player.songlist.InfoViewModel
import be.florien.anyflow.feature.player.songlist.SelectPlaylistViewModel
import be.florien.anyflow.feature.player.songlist.SongListViewModel
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
    @ViewModelKey(SelectFilterTypeViewModel::class)
    abstract fun bindsSelectFilterTypeViewModel(viewModel: SelectFilterTypeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SelectFilterAlbumViewModel::class)
    abstract fun bindsSelectFilterFragmentAlbumVM(viewModel: SelectFilterAlbumViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SelectFilterArtistViewModel::class)
    abstract fun bindsSelectFilterFragmentArtistVM(viewModel: SelectFilterArtistViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SelectFilterGenreViewModel::class)
    abstract fun bindsSelectFilterFragmentGenreVM(viewModel: SelectFilterGenreViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SelectFilterSongViewModel::class)
    abstract fun bindsSelectFilterFragmentSongVM(viewModel: SelectFilterSongViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SelectFilterPlaylistViewModel::class)
    abstract fun bindsSelectFilterFragmentPlaylistVM(viewModel: SelectFilterPlaylistViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SelectFilterDownloadedViewModel::class)
    abstract fun bindsSelectFilterFragmentDownloadedVM(viewModel: SelectFilterDownloadedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SavedFilterGroupViewModel::class)
    abstract fun bindsSavedFilterGroupVM(viewModel: SavedFilterGroupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DisplayFilterViewModel::class)
    abstract fun bindsDisplayFilterFragmentVM(viewModel: DisplayFilterViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(InfoViewModel::class)
    abstract fun bindsInfoFragmentVM(viewModel: InfoViewModel): ViewModel

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