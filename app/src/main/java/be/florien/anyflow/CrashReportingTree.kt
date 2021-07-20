package be.florien.anyflow

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {

    /**
     * Overridden Methods: DebugTree
     */

    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (message.isNotBlank() && priority > Log.DEBUG) {
            FirebaseCrashlytics.getInstance().log(message)
        }
        if (throwable != null && priority >= Log.ERROR) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
        when (priority) {
            Log.VERBOSE -> Log.v(tag, message, throwable)
            Log.DEBUG -> Log.d(tag, message, throwable)
            Log.INFO -> Log.i(tag, message, throwable)
            Log.WARN -> Log.w(tag, message, throwable)
            Log.ERROR -> Log.e(tag, message, throwable)
        }
    }
}
