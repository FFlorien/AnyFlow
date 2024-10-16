package be.florien.anyflow.feature.shortcut.ui.di

import be.florien.anyflow.architecture.di.ActivityScope
import be.florien.anyflow.feature.shortcut.ui.ShortcutsActivity
import dagger.Subcomponent

@Subcomponent(modules = [ShortcutViewModelModule::class])
@ActivityScope
interface ShortcutActivityComponent {

    fun inject(shortcutsActivity: ShortcutsActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): ShortcutActivityComponent
    }

}
