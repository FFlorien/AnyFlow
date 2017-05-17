package be.florien.ampacheplayer.di

import android.app.Activity
import be.florien.ampacheplayer.manager.*
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
interface ApplicationComponent {
    fun inject(authenticationManager: AuthenticationManager)
    fun inject(dataManager: DataManager)
    fun inject(connection: AmpacheConnection)
    fun inject(connectActivityVM: ConnectActivityVM)
    fun inject(playerActivityVM: PlayerActivityVM)

    fun getAmpacheApi(): AmpacheApi
}