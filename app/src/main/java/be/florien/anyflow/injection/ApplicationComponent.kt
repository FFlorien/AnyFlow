package be.florien.anyflow.injection

import android.app.Application
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.feature.auth.domain.di.NonAuthModule
import be.florien.anyflow.feature.auth.ui.server.ServerViewModel
import be.florien.anyflow.tags.local.di.DataLocalModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


/**
 * Component for the whole application, including at startup, when user is without a server or an account
 */
@Singleton
@Component(
    modules = [
        // ProvideModule
        ApplicationModule::class,
        ApplicationBindsModule::class,
        // ProvideModule from features
        DataLocalModule::class,
        NonAuthModule::class
    ]
)
interface ApplicationComponent {

    fun inject(anyFlowApp: AnyFlowApp)
    fun inject(anyFlowApp: ServerViewModel)

    fun serverComponentBuilder(): ServerComponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }
}