package be.florien.anyflow.injection

import android.app.Application
import androidx.media3.common.util.UnstableApi
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.feature.auth.ServerViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@UnstableApi
@Component(modules = [ApplicationModule::class, ApplicationWideModule::class])
interface ApplicationComponent {

    fun inject(anyFlowApp: AnyFlowApp)

    fun inject(serverViewModel: ServerViewModel)

    fun serverComponentBuilder(): ServerComponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }
}