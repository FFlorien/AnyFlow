package be.florien.ampacheplayer.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import be.florien.ampacheplayer.AmpacheApp
import kotlin.reflect.KClass

/**
 * Extension functions/properties for Context
 */
val Context.ampacheApp: AmpacheApp
    get() = this.applicationContext as AmpacheApp

val Activity.ampacheApp: AmpacheApp
    get() = this.applicationContext as AmpacheApp

fun Context.startActivity(activityClass: KClass<*>, flags: Int) {
    val intent = Intent(this, activityClass.java)
    intent.flags = flags
    startActivity(intent)
}

fun Context.startActivity(activityClass: KClass<*>) {
    startActivity(Intent(this, activityClass.java))
}