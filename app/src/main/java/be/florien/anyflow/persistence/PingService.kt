package be.florien.anyflow.persistence

import android.app.job.JobParameters
import android.app.job.JobService
import be.florien.anyflow.persistence.server.AmpacheConnection
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class PingService : JobService() {

    @Inject
    lateinit var ampacheConnection: AmpacheConnection

    private var subscription: Disposable? = null

    override fun onStartJob(p0: JobParameters?): Boolean {
        (application as be.florien.anyflow.AnyFlowApp).userComponent?.inject(this)
        subscription = ampacheConnection
                .ping()
                .doOnError {
                    ampacheConnection.reconnect(Observable.fromCallable {
                        subscription?.dispose()
                        jobFinished(p0, true)
                    })
                }
                .doOnComplete {
                    subscription?.dispose()
                    jobFinished(p0, true)
                }
                .subscribe()
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        subscription?.dispose()
        return true
    }
}