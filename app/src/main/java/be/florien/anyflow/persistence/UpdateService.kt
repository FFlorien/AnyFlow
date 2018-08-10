package be.florien.anyflow.persistence

import android.app.IntentService
import android.content.Intent
import be.florien.anyflow.exception.NoServerException
import be.florien.anyflow.exception.SessionExpiredException
import be.florien.anyflow.exception.WrongIdentificationPairException
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class UpdateService
@Inject constructor() : IntentService("UPDATE_SERVICE") {

    @Inject
    lateinit var persistenceManager: PersistenceManager

    companion object {
        const val UPDATE_ALL = "UPDATE_ALL"
    }

    override fun onCreate() {
        super.onCreate()
        (application as be.florien.anyflow.AnyFlowApp).userComponent?.inject(this)
    }

    override fun onHandleIntent(intent: Intent) {
        when (intent.dataString) {
            be.florien.anyflow.persistence.UpdateService.Companion.UPDATE_ALL -> {
                persistenceManager.updateSongs()
                        .andThen(persistenceManager.updateAlbums())
                        .andThen(persistenceManager.updateArtists())
                        .doOnError { throwable ->
                            when (throwable) {
                                is SessionExpiredException -> {
                                    Timber.i(throwable, "The session token is expired")
//                                    navigator.goToConnection()
                                }
                                is WrongIdentificationPairException -> {
                                    Timber.i(throwable, "Couldn't reconnect the user: wrong user/pwd")
//                                    navigator.goToConnection()
                                }
                                is SocketTimeoutException, is NoServerException -> {
                                    Timber.e(throwable, "Couldn't connect to the webservice")
//                                    displayHelper.notifyUserAboutError("Couldn't connect to the webservice")
                                }
                                else -> {
                                    Timber.e(throwable, "Unknown error")
//                                    displayHelper.notifyUserAboutError("Couldn't connect to the webservice")
//                                    navigator.goToConnection()
                                }
                            }
                        }
                        .subscribeOn(Schedulers.io())
                        .subscribe()
            }
        }
    }
}