package be.florien.anyflow

import com.facebook.stetho.Stetho

/**
 * Application class used for initialization of many libraries
 */
class AnyFlowAppDebug : AnyFlowApp() {

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
    }
}