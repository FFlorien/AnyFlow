package be.florien.ampacheplayer.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.extension.getAmpacheApp

/**
 * Created by florien on 17/05/17.
 */

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getAmpacheApp().initActivityInjection(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        getAmpacheApp().releaseActivityInjection()
    }
}