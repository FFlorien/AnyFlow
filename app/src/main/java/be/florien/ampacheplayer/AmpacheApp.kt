package be.florien.ampacheplayer

import android.app.Application
import android.util.Log
import com.facebook.stetho.Stetho
import io.realm.Realm
import timber.log.Timber

/**
 * Application class used for initialization of many libraries
 */
abstract class AmpacheApp : Application() {
    lateinit var applicationComponent: IApplicationComponent
        protected set

    override fun onCreate() {
        super.onCreate()
        initLibrariesForBuildType()
    }

    abstract fun initLibrariesForBuildType()
}