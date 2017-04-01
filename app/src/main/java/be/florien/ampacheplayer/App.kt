package be.florien.ampacheplayer

import android.app.Application
import be.florien.ampacheplayer.di.AmpacheComponent
import be.florien.ampacheplayer.di.AndroidModule
import be.florien.ampacheplayer.di.DaggerAmpacheComponent
import be.florien.ampacheplayer.di.DataModule
import com.facebook.stetho.Stetho
import io.realm.Realm


/**
 * Application used for initialization of many libraries
 */
class App : Application() {

    companion object {
        lateinit var ampacheComponent: AmpacheComponent
    }

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
        Realm.init(this)
        ampacheComponent =
                DaggerAmpacheComponent
                        .builder()
                        .dataModule(DataModule())
                        .androidModule(AndroidModule(this))
                        .build()
    }
}