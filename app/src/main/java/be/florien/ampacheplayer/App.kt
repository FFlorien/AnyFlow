package be.florien.ampacheplayer

import android.app.Application
import be.florien.ampacheplayer.di.*
import com.facebook.stetho.Stetho
import io.realm.Realm


/**
 * Application class used for initialization of many libraries
 */
class App : Application() {

    companion object {
        lateinit var applicationComponent: ApplicationComponent
    }

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
        Realm.init(this)
        applicationComponent =
                DaggerApplicationComponent
                        .builder()
                        .dataModule(DataModule())
                        .androidModule(AndroidModule(this))
                        .build()
    }
}