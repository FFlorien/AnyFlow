package be.florien.anyflow.injection

import be.florien.anyflow.feature.player.ui.info.song.shortcuts.ShortcutsActivity
import dagger.Subcomponent

@Subcomponent(modules = [ViewModelModule::class])
@ActivityScope
interface ShortcutsComponent {

    fun inject(shortcutsActivity: ShortcutsActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): ShortcutsComponent
    }

}
