package be.florien.anyflow.injection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.architecture.di.AnyFlowViewModelFactory
import be.florien.anyflow.architecture.di.ViewModelKey
import be.florien.anyflow.feature.player.ui.PlayerViewModel
import be.florien.anyflow.feature.player.ui.filters.CurrentFilterViewModel
import be.florien.anyflow.feature.player.ui.filters.saved.SavedFilterGroupViewModel
import be.florien.anyflow.feature.song.ui.SongInfoViewModel
import be.florien.anyflow.feature.shortcut.ui.ShortcutsViewModel
import be.florien.anyflow.feature.player.ui.songlist.SongListViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

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
    abstract fun bindsViewModelFactory(factory: AnyFlowViewModelFactory): ViewModelProvider.Factory
}