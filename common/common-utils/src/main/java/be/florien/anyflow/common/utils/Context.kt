package be.florien.anyflow.common.utils

import android.content.Context
import android.content.Intent
import kotlin.reflect.KClass

/**
 * Extension functions/properties for Context
 */

fun Context.startActivity(activityClass: KClass<*>) {
    startActivity(Intent(this, activityClass.java))
}