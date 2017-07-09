package be.florien.ampacheplayer.di

import be.florien.ampacheplayer.view.viewmodel.ConnectActivityVM
import be.florien.ampacheplayer.view.viewmodel.PlayerActivityVM
import dagger.Subcomponent

/**
 * Injection for Activities lifecycle
 */
@ActivityScope
@Subcomponent(modules = arrayOf(ActivityModule::class))
interface ActivityComponent {
    fun inject(connectActivityVM: ConnectActivityVM)
    fun inject(playerActivityVM: PlayerActivityVM)
}