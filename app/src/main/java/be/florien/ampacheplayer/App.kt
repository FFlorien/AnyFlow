package be.florien.ampacheplayer

import android.app.Application
import be.florien.ampacheplayer.di.AmpacheComponent
import be.florien.ampacheplayer.di.DaggerAmpacheComponent
import be.florien.ampacheplayer.di.NetworkDataModule
import com.facebook.stetho.Stetho


/**
 * Application used for initialization of many libraries
 */
class App : Application() {

    companion object {
        lateinit var ampacheComponent : AmpacheComponent
    }

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
        ampacheComponent = DaggerAmpacheComponent.builder().dataModule(NetworkDataModule()).build()
    }
}