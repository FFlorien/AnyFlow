package be.florien.anyflow.extension

import android.content.SharedPreferences
import java.util.*

/**
 * Extension function for preferences */

fun SharedPreferences.getDate(key: String, long: Long): Calendar = Calendar.getInstance().apply {
    timeInMillis = getLong(key, long)
}

fun SharedPreferences.applyPutLong(key: String, long: Long) {
    edit().putLong(key, long).apply()
}

fun SharedPreferences.applyPutInt(key: String, int: Int) {
    edit().putInt(key, int).apply()
}