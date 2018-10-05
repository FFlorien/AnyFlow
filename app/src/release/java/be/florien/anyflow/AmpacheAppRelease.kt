package be.florien.anyflow

import com.facebook.stetho.Stetho
import timber.log.Timber

/**
 * Created by FlamentF on 17-Jan-18.
 */

class AnyFlowAppRelease : AnyFlowApp() {

    override fun initLibrariesForBuildType() {
        Stetho.initializeWithDefaults(this)
        Timber.plant(Timber.DebugTree())
        applicationComponent = DaggerApplicationComponent
                .builder()
                .application(this)
                .applicationModule(ApplicationModule())
                .build()
    }
}