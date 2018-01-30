package be.florien.ampacheplayer

import com.facebook.stetho.Stetho

/**
 * Application class used for initialization of many libraries
 */
class AmpacheAppDebug : AmpacheApp() {

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
    }
}