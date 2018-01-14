package be.florien.ampacheplayer

import android.app.Application
import android.util.Log
import com.facebook.stetho.Stetho
import io.realm.Realm
import timber.log.Timber

/**
 * Application class used for initialization of many libraries
 */
class AmpacheApp : Application() {
    lateinit var applicationComponent: ApplicationComponent
        private set

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
        Realm.init(this)
        Timber.plant(Timber.DebugTree())
        applicationComponent = DaggerApplicationComponent
                .builder()
                .application(this)
                .applicationModule(ApplicationModule())
                .build()
    }
}