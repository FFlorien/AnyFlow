package be.florien.ampacheplayer.extension

import android.content.Context
import android.content.Intent
import be.florien.ampacheplayer.AmpacheApp
import kotlin.reflect.KClass

/**
 * Created by florien on 28/05/17.
 */
fun Context.getAmpacheApp() : AmpacheApp = this.applicationContext as AmpacheApp

fun Context.startActivity(activityClass: KClass<*>, flags: Int) {
    val intent = Intent(this, activityClass.java)
    intent.flags = flags
    startActivity(intent)
}
fun Context.startActivity(activityClass: KClass<*>) {
    startActivity(Intent(this, activityClass.java))
}