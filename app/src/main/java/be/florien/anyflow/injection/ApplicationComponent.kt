package be.florien.anyflow.injection

import android.app.Application
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.extension.MyAppGlideModule
import be.florien.anyflow.feature.connect.ServerViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(modules = [ApplicationModule::class, ApplicationWideModule::class])
interface ApplicationComponent {

    fun inject(anyFlowApp: AnyFlowApp)

    fun inject(serverViewModel: ServerViewModel)
    fun inject(myAppGlideModule: MyAppGlideModule)

    fun serverComponentBuilder(): ServerComponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }
}