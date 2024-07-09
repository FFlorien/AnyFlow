package be.florien.anyflow.injection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.architecture.di.AnyFlowViewModelFactory
import be.florien.anyflow.architecture.di.ViewModelKey
import be.florien.anyflow.feature.player.ui.PlayerViewModel
import be.florien.anyflow.feature.player.ui.filters.CurrentFilterViewModel
import be.florien.anyflow.feature.player.ui.info.song.SongInfoViewModel
import be.florien.anyflow.feature.player.ui.info.song.shortcuts.ShortcutsViewModel
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