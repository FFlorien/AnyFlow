package be.florien.ampacheplayer.di

import be.florien.ampacheplayer.manager.AmpacheConnection
import be.florien.ampacheplayer.manager.AmpacheDatabase
import be.florien.ampacheplayer.manager.AuthenticationManager
import be.florien.ampacheplayer.manager.DataManager
import be.florien.ampacheplayer.view.viewmodel.ConnectActivityVM
import be.florien.ampacheplayer.view.viewmodel.PlayerActivityVM
import dagger.Component
import javax.inject.Singleton

/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(
        modules = arrayOf(
                DataModule::class,
                AndroidModule::class
        )
)
interface AmpacheComponent {
    fun inject(mainActivityVm: ConnectActivityVM)
    fun inject(connection: AmpacheConnection)
    fun inject(dataManager: DataManager)
    fun inject(authenticationManager: AuthenticationManager)
    fun inject(playerActivityVM: PlayerActivityVM)
}