package be.florien.ampacheplayer

import android.app.Activity
import android.app.Application
import be.florien.ampacheplayer.di.*
import com.facebook.stetho.Stetho
import io.realm.Realm


/**
 * Application class used for initialization of many libraries
 */
class AmpacheApp : Application() {
    lateinit var applicationComponent: ApplicationComponent
        private set
    var activityComponent: ActivityComponent? = null
        private set

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
        Realm.init(this)
        applicationComponent =
                DaggerApplicationComponent
                        .builder()
                        .dataModule(DataModule())
                        .androidModule(AndroidModule(this))
                        .build()
    }

    fun initActivityInjection(activity: Activity) {
        activityComponent = applicationComponent.plus(ActivityModule(activity))
    }

    fun releaseActivityInjection() {
        activityComponent = null
    }

}