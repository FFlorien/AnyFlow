package be.florien.ampacheplayer

import android.app.IntentService
import android.content.Intent
import be.florien.ampacheplayer.exception.NoServerException
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.persistence.PersistenceManager
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
        (application as AmpacheApp).userComponent?.inject(this)
    }

    override fun onHandleIntent(intent: Intent) {
        when (intent.dataString) {
            UPDATE_ALL -> {
                persistenceManager.updateSongs()
                        .andThen(persistenceManager.updateAlbums())
                        .andThen(persistenceManager.updateArtists())
                        .andThen(persistenceManager.updateGenres())
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
                        .subscribe()
            }
        }
    }
}