package be.florien.anyflow

import timber.log.Timber

/**
 * Created by FlamentF on 17-Jan-18.
 */

class AnyFlowAppRelease : AnyFlowApp() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}