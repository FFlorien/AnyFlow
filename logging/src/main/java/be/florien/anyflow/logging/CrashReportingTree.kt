package be.florien.anyflow.logging

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {

    /**
     * Overridden Methods: DebugTree
     */

    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t == null) {
            when (priority) {
                Log.VERBOSE -> Log.v(tag, message)
                Log.DEBUG -> Log.d(tag, message)
                Log.INFO -> Log.i(tag, message)
                Log.WARN -> Log.w(tag, message)
                Log.ERROR -> Log.e(tag, message)
            }
        } else {
            when (priority) {
                Log.VERBOSE -> Log.v(tag, message, t)
                Log.DEBUG -> Log.d(tag, message, t)
                Log.INFO -> Log.i(tag, message, t)
                Log.WARN -> Log.w(tag, message, t)
                Log.ERROR -> Log.e(tag, message, t)
            }
        }
        if (message.isNotBlank() && priority > Log.DEBUG) {
            FirebaseCrashlytics.getInstance().log(message)
        }
        if (t != null && priority >= Log.ERROR) {
            FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}
