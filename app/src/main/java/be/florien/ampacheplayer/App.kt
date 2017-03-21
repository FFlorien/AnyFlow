package be.florien.ampacheplayer

import android.app.Application
import com.facebook.stetho.Stetho


/**
 * Created by florien on 10/03/17.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
    }
}