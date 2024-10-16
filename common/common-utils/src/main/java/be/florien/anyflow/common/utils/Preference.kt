package be.florien.anyflow.common.utils

import android.content.SharedPreferences

/**
 * Extension function for preferences */

fun SharedPreferences.applyPutLong(key: String, long: Long) {
    edit().putLong(key, long).apply()
}

fun SharedPreferences.applyPutInt(key: String, int: Int) {
    edit().putInt(key, int).apply()
}