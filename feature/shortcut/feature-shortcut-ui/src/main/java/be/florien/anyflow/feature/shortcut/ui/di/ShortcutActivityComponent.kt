package be.florien.anyflow.feature.shortcut.ui.di

import be.florien.anyflow.architecture.di.ActivityScope
import dagger.Subcomponent

@Subcomponent
@ActivityScope
interface ShortcutActivityComponent {

    fun inject(shortcutsActivity: be.florien.anyflow.feature.shortcut.ui.ShortcutsActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): ShortcutActivityComponent
    }

}
