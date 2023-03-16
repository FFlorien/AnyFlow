package be.florien.anyflow.data

import android.app.job.JobParameters
import android.app.job.JobService
import be.florien.anyflow.data.server.AmpacheAuthSource
import be.florien.anyflow.data.server.exception.SessionExpiredException
import be.florien.anyflow.extension.eLog
import kotlinx.coroutines.*
import javax.inject.Inject

class PingService
@Inject constructor() : JobService() {

    @Inject
    lateinit var ampacheAuthSource: AmpacheAuthSource

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        eLog(throwable, "Received an exception in PingService's scope")
    }
    private val serviceScope = CoroutineScope(Dispatchers.Main + exceptionHandler)

    override fun onStartJob(p0: JobParameters?): Boolean {
        (application as be.florien.anyflow.AnyFlowApp).serverComponent?.inject(this)
        serviceScope.launch(Dispatchers.IO) {
            try {
                val pingResponse = ampacheAuthSource.ping()
                if (pingResponse.error.errorCode.div(100) == 4) {
                    throw SessionExpiredException("Ping thrown an error")
                }
            } catch (exception: Exception) {
                ampacheAuthSource.reconnect {
                    jobFinished(p0, true)
                }
            }
            jobFinished(p0, true)
        }
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        serviceScope.cancel()
        return true
    }
}