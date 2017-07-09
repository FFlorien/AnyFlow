package be.florien.ampacheplayer.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.extension.ampacheApp

/**
 * Base activity used to handle activity lifecycle in DI
 */

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ampacheApp.initActivityInjection(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ampacheApp.releaseActivityInjection()
    }
}