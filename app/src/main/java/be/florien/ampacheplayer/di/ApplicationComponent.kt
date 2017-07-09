package be.florien.ampacheplayer.di

import be.florien.ampacheplayer.player.PlayerService
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
    fun plus(activity: ActivityModule): ActivityComponent
    fun inject(playerService: PlayerService)
}