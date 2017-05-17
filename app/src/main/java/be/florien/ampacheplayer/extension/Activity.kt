package be.florien.ampacheplayer.extension

import android.app.Activity
import android.content.Intent
import kotlin.reflect.KClass

/**
 * Created by florien on 18/05/17.
 */
fun Activity.startActivity(activityClass: KClass<*>) {
    startActivity(Intent(this, activityClass.java))
}