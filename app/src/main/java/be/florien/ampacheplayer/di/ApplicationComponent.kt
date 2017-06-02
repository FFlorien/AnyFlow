package be.florien.ampacheplayer.di

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
}