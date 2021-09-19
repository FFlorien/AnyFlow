package be.florien.anyflow.data

import android.app.job.JobParameters
import android.app.job.JobService
import be.florien.anyflow.CrashReportingTree
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.server.exception.SessionExpiredException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class PingService
@Inject constructor() : JobService() {

    @Inject
    lateinit var ampacheConnection: AmpacheConnection

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(serviceJob)

    override fun onStartJob(p0: JobParameters?): Boolean {
        (application as be.florien.anyflow.AnyFlowApp).userComponent?.inject(this)
        Timber.plant(CrashReportingTree())
        serviceScope.launch {
            try {
                val it = ampacheConnection.ping()
                if (it.error.code.div(100) == 4) {
                    throw SessionExpiredException("Ping thrown an error")
                }
            } catch (exception: Exception) {
                ampacheConnection.reconnect {
                    jobFinished(p0, true)
                }
            }
            jobFinished(p0, true)
        }
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        return true
    }
}