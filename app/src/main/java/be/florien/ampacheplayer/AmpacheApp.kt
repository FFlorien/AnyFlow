package be.florien.ampacheplayer

import android.app.Application
import android.util.Log
import be.florien.ampacheplayer.di.AndroidModule
import be.florien.ampacheplayer.di.ApplicationComponent
import be.florien.ampacheplayer.di.DaggerApplicationComponent
import be.florien.ampacheplayer.di.DataModule
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
        Timber.plant(debugTree)
        applicationComponent =
                DaggerApplicationComponent
                        .builder()
                        .dataModule(DataModule())
                        .androidModule(AndroidModule(this))
                        .build()
    }

    object debugTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
            Log.println(priority, tag, "Message: $message \n ${Log.getStackTraceString(t)}")
        }

    }
}