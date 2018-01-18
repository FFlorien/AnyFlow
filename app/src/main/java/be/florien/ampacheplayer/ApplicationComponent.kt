package be.florien.ampacheplayer

import android.app.Application
import android.content.SharedPreferences
import be.florien.ampacheplayer.api.AmpacheConnection
import be.florien.ampacheplayer.view.ActivityComponent
import be.florien.ampacheplayer.view.Navigator
import dagger.BindsInstance
import dagger.Component
import io.realm.Realm
import javax.inject.Singleton


/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(modules = [(ApplicationModule::class), (ApplicationWideModule::class)])
interface ApplicationComponent  {

    fun activityComponentBuilder(): ActivityComponent.Builder

    fun realm(): Realm
    fun ampacheConnection(): AmpacheConnection
    fun preferences(): SharedPreferences
    fun navigator(): Navigator

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }

}