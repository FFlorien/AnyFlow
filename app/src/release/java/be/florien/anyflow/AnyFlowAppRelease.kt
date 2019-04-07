package be.florien.anyflow

import com.facebook.stetho.Stetho
import timber.log.Timber

/**
 * Created by FlamentF on 17-Jan-18.
 */

class AnyFlowAppRelease : AnyFlowApp() {

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
        Timber.plant(Timber.DebugTree())
    }
}