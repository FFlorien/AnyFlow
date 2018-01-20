package be.florien.ampacheplayer

import android.app.Application
import android.util.Log
import be.florien.ampacheplayer.user.UserComponent
import com.facebook.stetho.Stetho
import io.realm.Realm
import timber.log.Timber

/**
 * Application class used for initialization of many libraries
 */
abstract class AmpacheApp : Application() {
    lateinit var applicationComponent: ApplicationComponent
        protected set
    var userComponent: UserComponent? = null

    override fun onCreate() {
        super.onCreate()
        initLibrariesForBuildType()
    }

    abstract fun initLibrariesForBuildType()
}