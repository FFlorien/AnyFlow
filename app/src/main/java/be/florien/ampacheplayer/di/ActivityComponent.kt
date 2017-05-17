package be.florien.ampacheplayer.di

import be.florien.ampacheplayer.manager.AmpacheConnection
import dagger.Component

/**
 * Created by florien on 17/05/17.
 */
@ActivityScope
@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(ActivityModule::class))
interface ActivityComponent {
    fun inject(connection: AmpacheConnection)

}