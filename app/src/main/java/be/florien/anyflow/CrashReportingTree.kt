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
        if (throwable == null) {
            when (priority) {
                Log.VERBOSE -> Log.v(tag, message)
                Log.DEBUG -> Log.d(tag, message)
                Log.INFO -> Log.i(tag, message)
                Log.WARN -> Log.w(tag, message)
                Log.ERROR -> Log.e(tag, message)
            }
        } else {
            when (priority) {
                Log.VERBOSE -> Log.v(tag, message, throwable)
                Log.DEBUG -> Log.d(tag, message, throwable)
                Log.INFO -> Log.i(tag, message, throwable)
                Log.WARN -> Log.w(tag, message, throwable)
                Log.ERROR -> Log.e(tag, message, throwable)
            }
        }
        if (message.isNotBlank() && priority > Log.DEBUG) {
            FirebaseCrashlytics.getInstance().log(message)
        }
        if (throwable != null && priority >= Log.ERROR) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }
}
