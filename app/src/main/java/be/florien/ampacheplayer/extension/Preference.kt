package be.florien.ampacheplayer.extension

import android.content.SharedPreferences

/**
 * Created by florien on 8/05/17.
 */


fun SharedPreferences.applyPutLong(key: String, long: Long) {
    edit().putLong(key, long).apply()
}

fun SharedPreferences.applyPutString(key: String, string: String) {
    edit().putString(key, string).apply()
}