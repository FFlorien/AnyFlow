package be.florien.anyflow.common.ui

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.RequiresApi


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