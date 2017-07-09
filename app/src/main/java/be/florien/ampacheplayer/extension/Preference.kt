package be.florien.ampacheplayer.extension

import android.content.SharedPreferences

/**
 * Extension function for preferences */

fun SharedPreferences.applyPutLong(key: String, long: Long) {
    edit().putLong(key, long).apply()
}

fun SharedPreferences.applyPutString(key: String, string: String) {
    edit().putString(key, string).apply()
}