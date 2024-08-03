package be.florien.anyflow.extension

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.RequiresApi
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

fun Activity.getDisplayWidth(): Int {
    return if (Build.VERSION.SDK_INT >= 30) {
        getDisplayWidthNew()
    } else {
        getDisplayWidthLegacy()
    }
}

@Suppress("DEPRECATION")
private fun Activity.getDisplayWidthLegacy(): Int {
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}

@RequiresApi(Build.VERSION_CODES.R)
private fun Activity.getDisplayWidthNew() = windowManager.currentWindowMetrics.bounds.width()