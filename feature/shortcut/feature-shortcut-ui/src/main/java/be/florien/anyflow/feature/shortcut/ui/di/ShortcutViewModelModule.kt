package be.florien.anyflow.feature.shortcut.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.common.di.ViewModelKey
import be.florien.anyflow.feature.shortcut.ui.ShortcutsViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ShortcutViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(ShortcutsViewModel::class)
    abstract fun bindsShortcutViewModel(viewModel: ShortcutsViewModel): ViewModel
}