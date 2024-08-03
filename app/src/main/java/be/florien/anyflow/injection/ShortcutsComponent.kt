package be.florien.anyflow.injection

import be.florien.anyflow.architecture.di.ActivityScope
import be.florien.anyflow.feature.player.ui.info.song.shortcuts.ShortcutsActivity
import dagger.Subcomponent

@Subcomponent
@ActivityScope
interface ShortcutsComponent {

    fun inject(shortcutsActivity: ShortcutsActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): ShortcutsComponent
    }

}
