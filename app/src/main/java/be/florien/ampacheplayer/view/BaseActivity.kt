package be.florien.ampacheplayer.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.App
import be.florien.ampacheplayer.di.*

/**
 * Created by florien on 17/05/17.
 */

open class BaseActivity : AppCompatActivity() {

    companion object {
        lateinit var activityComponent: ActivityComponent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BaseActivity.activityComponent =
                DaggerActivityComponent
                        .builder()
                        .applicationComponent(App.applicationComponent)
                        .activityModule(ActivityModule(this))
                        .build()
    }
}