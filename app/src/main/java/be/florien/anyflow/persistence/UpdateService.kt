package be.florien.anyflow.persistence

import android.app.IntentService
import android.content.Intent
import be.florien.anyflow.exception.NoServerException
import be.florien.anyflow.exception.SessionExpiredException
import be.florien.anyflow.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.extension.iLog
import io.reactivex.schedulers.Schedulers
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
            be.florien.anyflow.persistence.UpdateService.UPDATE_ALL -> {
                persistenceManager.updateArtists()
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
                        .subscribeOn(Schedulers.io())
                        .subscribe()
            }
        }
    }
}