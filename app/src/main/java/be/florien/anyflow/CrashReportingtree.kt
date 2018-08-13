package be.florien.anyflow

import com.crashlytics.android.Crashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {

    /**
     * Overridden Methods: DebugTree
     */

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (message.isNotBlank()) {
            Crashlytics.log(priority, tag, message)
        }
        if (t != null) {
            Crashlytics.logException(t)
        }
    }
}
