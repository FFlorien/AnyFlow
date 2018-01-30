package be.florien.ampacheplayer

import android.app.Application
import be.florien.ampacheplayer.user.UserComponent
import be.florien.ampacheplayer.view.connect.ConnectComponent
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(modules = [(MockUpModule::class), (ApplicationWideModule::class)])
interface ApplicationComponent  {

    fun inject(ampacheApp: AmpacheApp)

    fun connectComponentBuilder(): ConnectComponent.Builder
    fun userComponentBuilder(): UserComponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }
}