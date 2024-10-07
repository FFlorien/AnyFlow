package be.florien.anyflow.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import be.florien.anyflow.AnyFlowApp
import kotlin.reflect.KClass

/**
 * Extension functions/properties for Context
 */
val Activity.anyFlowApp: AnyFlowApp
    get() = this.applicationContext as AnyFlowApp

val Fragment.anyFlowApp: AnyFlowApp
    get() = this.requireActivity().applicationContext as AnyFlowApp

fun Context.startActivity(activityClass: KClass<*>) {
    startActivity(Intent(this, activityClass.java))
}