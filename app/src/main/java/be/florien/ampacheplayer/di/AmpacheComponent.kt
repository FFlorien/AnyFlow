package be.florien.ampacheplayer.di

import be.florien.ampacheplayer.manager.AmpacheConnection
import be.florien.ampacheplayer.manager.AmpacheDatabase
import be.florien.ampacheplayer.manager.AuthenticationManager
import be.florien.ampacheplayer.manager.DataManager
import be.florien.ampacheplayer.view.viewmodel.MainActivityVM
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
    fun inject(mainActivityVm: MainActivityVM)
    fun inject(connection: AmpacheConnection)
    fun inject(dataManager: DataManager)
    fun inject(authenticationManager: AuthenticationManager)
}