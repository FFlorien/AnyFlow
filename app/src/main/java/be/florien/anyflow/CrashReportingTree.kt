package be.florien.anyflow

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {

    /**
     * Overridden Methods: DebugTree
     */

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (message.isNotBlank()) {
            Crashlytics.log(priority, tag, message)
        }
        if (throwable != null && priority >= Log.ERROR) {
            Crashlytics.logException(throwable)
        }
    }
}
