package be.florien.anyflow.di

import android.app.Application
import be.florien.anyflow.user.UserComponent
import be.florien.anyflow.view.connect.ConnectComponent
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(modules = [(be.florien.anyflow.di.ApplicationModule::class), (be.florien.anyflow.di.ApplicationWideModule::class)])
interface ApplicationComponent  {

    fun inject(anyFlowApp: be.florien.anyflow.AnyFlowApp)

    fun connectComponentBuilder(): ConnectComponent.Builder
    fun userComponentBuilder(): UserComponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): be.florien.anyflow.di.ApplicationComponent.Builder

        fun build(): be.florien.anyflow.di.ApplicationComponent
    }
}