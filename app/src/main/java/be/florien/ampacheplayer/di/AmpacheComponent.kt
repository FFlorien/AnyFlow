package be.florien.ampacheplayer.di

import be.florien.ampacheplayer.model.manager.AmpacheConnection
import be.florien.ampacheplayer.model.manager.AmpacheDatabase
import be.florien.ampacheplayer.view.viewmodel.MainActivityVM
import dagger.Component
import javax.inject.Singleton

/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(
        modules = arrayOf(
                NetworkModule::class
        )
)
interface AmpacheComponent {
    fun inject(mainActivityVm: MainActivityVM)
    fun inject(connection: AmpacheConnection)
    fun inject(connection: AmpacheDatabase)
}