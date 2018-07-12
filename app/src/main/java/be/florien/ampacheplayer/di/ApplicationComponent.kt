package be.florien.ampacheplayer.di

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
@Component(modules = [(be.florien.ampacheplayer.di.ApplicationModule::class), (be.florien.ampacheplayer.di.ApplicationWideModule::class)])
interface ApplicationComponent  {

    fun inject(ampacheApp: be.florien.ampacheplayer.AmpacheApp)

    fun connectComponentBuilder(): ConnectComponent.Builder
    fun userComponentBuilder(): UserComponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): be.florien.ampacheplayer.di.ApplicationComponent.Builder

        fun build(): be.florien.ampacheplayer.di.ApplicationComponent
    }
}