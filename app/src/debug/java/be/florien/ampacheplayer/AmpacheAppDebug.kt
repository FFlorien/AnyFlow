package be.florien.ampacheplayer

import com.facebook.stetho.Stetho
import io.realm.Realm
import timber.log.Timber

/**
 * Created by FlamentF on 17-Jan-18.
 */

class AmpacheAppDebug : AmpacheApp() {

    override fun initLibrariesForBuildType() {
        Stetho.initializeWithDefaults(this)
        Realm.init(this)
        Timber.plant(Timber.DebugTree())
        applicationComponent = DaggerApplicationComponent
                .builder()
                .application(this)
                .build()
    }
}