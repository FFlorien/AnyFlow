package be.florien.anyflow

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {

    /**
     * Overridden Methods: DebugTree
     */

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (message.isNotBlank() && priority > Log.DEBUG) {
            FirebaseCrashlytics.getInstance().log(message)
        }
        if (throwable != null && priority >= Log.ERROR) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }
}
