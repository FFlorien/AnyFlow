package be.florien.ampacheplayer

import android.app.Application
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.ActivityComponent
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(modules = [(MockUpModule::class), (ApplicationContextModule::class)])
interface MockUpComponent: IApplicationComponent {

    override fun activityComponentBuilder(): ActivityComponent.Builder

    override fun inject(playerService: PlayerService)

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun applicationModule(mockUpModule: MockUpModule): Builder

        fun build(): MockUpComponent
    }

}