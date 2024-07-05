package be.florien.anyflow.injection

import android.app.Application
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.data.local.di.DataLocalModule
import be.florien.anyflow.ui.di.ServerVmInjector
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(
    modules = [
        ApplicationModule::class,
        ApplicationWideModule::class,
        DataLocalModule::class
    ]
)
interface ApplicationComponent: ServerVmInjector {

    fun inject(anyFlowApp: AnyFlowApp)

    fun serverComponentBuilder(): ServerComponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }
}