package be.florien.anyflow.persistence

import android.app.job.JobParameters
import android.app.job.JobService
import be.florien.anyflow.exception.NoServerException
import be.florien.anyflow.exception.SessionExpiredException
import be.florien.anyflow.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.extension.iLog
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.net.SocketTimeoutException
import javax.inject.Inject

class UpdateService
@Inject constructor() : JobService() {
    @Inject
    lateinit var persistenceManager: PersistenceManager

    private var subscription: Disposable? = null

    override fun onStartJob(p0: JobParameters?): Boolean { //todo act upon errors
        (application as be.florien.anyflow.AnyFlowApp).userComponent?.inject(this)
        subscription = persistenceManager.updateArtists()
                .andThen(persistenceManager.updateAlbums())
                .andThen(persistenceManager.updateSongs())
                .doOnError { throwable ->
                    when (throwable) {
                        is SessionExpiredException -> {
                            this@UpdateService.iLog(throwable, "The session token is expired")
//                                    navigator.goToConnection()
                        }
                        is WrongIdentificationPairException -> {
                            this@UpdateService.iLog(throwable, "Couldn't reconnect the user: wrong user/pwd")
//                                    navigator.goToConnection()
                        }
                        is SocketTimeoutException, is NoServerException -> {
                            this@UpdateService.eLog(throwable, "Couldn't connect to the webservice")
//                                    displayHelper.notifyUserAboutError("Couldn't connect to the webservice")
                        }
                        else -> {
                            this@UpdateService.eLog(throwable, "Unknown error")
//                                    displayHelper.notifyUserAboutError("Couldn't connect to the webservice")
//                                    navigator.goToConnection()
                        }
                    }
                }
                .doOnComplete {
                    jobFinished(p0, true)
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        subscription?.dispose()
        return true
    }
}