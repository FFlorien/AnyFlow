package be.florien.anyflow.injection

import android.app.Application
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.data.user.UserComponent
import be.florien.anyflow.feature.connect.ConnectViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(modules = [ApplicationModule::class, ApplicationWideModule::class])
interface ApplicationComponent  {

    fun inject(anyFlowApp: AnyFlowApp)

    fun inject(connectViewModel: ConnectViewModel)

    fun userComponentBuilder(): UserComponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }
}