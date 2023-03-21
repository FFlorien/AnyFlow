package be.florien.anyflow.data

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.server.AmpacheAuthSource
import be.florien.anyflow.data.server.exception.NotAnAmpacheUrlException
import be.florien.anyflow.data.server.exception.SessionExpiredException
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.data.server.model.AmpacheAuthenticatedStatus
import be.florien.anyflow.data.server.model.AmpacheAuthentication
import be.florien.anyflow.data.server.model.AmpacheStatus
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ServerScope
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Manager for the ampache API server-side
 */
@ServerScope
open class AuthRepository
@Inject constructor(
    private val ampacheAuthSource: AmpacheAuthSource,
    private val authPersistence: AuthPersistence,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val COUNT_SONGS = "COUNT_SONGS"
        private const val COUNT_GENRES = "COUNT_GENRES"
        private const val COUNT_ALBUMS = "COUNT_ALBUMS"
        private const val COUNT_ARTIST = "COUNT_ARTIST"
        private const val COUNT_PLAYLIST = "COUNT_PLAYLIST"
        const val SERVER_UPDATE = "SERVER_UPDATE"
        const val SERVER_ADD = "SERVER_ADD"
        const val SERVER_CLEAN = "SERVER_CLEAN"
    }

    private var reconnectByUserPassword = 0

    val connectionStatusUpdater = MutableLiveData(ConnectionStatus.CONNECTED)

    /**
     * API calls : connection
     */
    suspend fun authenticate(user: String, password: String): AmpacheAuthentication {
        connectionStatusUpdater.postValue(ConnectionStatus.CONNEXION)
        try {
            val authentication = ampacheAuthSource.authenticate(user, password)
            when (authentication.error.errorCode) {
                401 -> {
                    connectionStatusUpdater.postValue(ConnectionStatus.WRONG_ID_PAIR)
                    throw WrongIdentificationPairException(authentication.error.errorMessage)
                }
                0 -> {
                    authPersistence.saveConnectionInfo(
                        user,
                        password,
                        authentication.auth,
                        TimeOperations.getDateFromAmpacheComplete(authentication.session_expire).timeInMillis
                    )
                    saveData(authentication)
                }
            }
            connectionStatusUpdater.postValue(ConnectionStatus.CONNECTED)
            return authentication
        } catch (exception: HttpException) {
            connectionStatusUpdater.postValue(ConnectionStatus.WRONG_SERVER_URL)
            throw NotAnAmpacheUrlException("The ampache server couldn't be found at provided url")
        } catch (exception: Exception) {
            eLog(exception, "Unknown error while trying to login")
            throw exception
        }
    }

    suspend fun authenticatedPing(): AmpacheAuthenticatedStatus {
        val authToken: String = authPersistence.authToken.secret
        try {
            val ping = ampacheAuthSource.authenticatedPing(authToken)
            if (ping.session_expire.isEmpty()) {
                return ping
            }

            saveData(ping)
            authPersistence.setNewAuthExpiration(TimeOperations.getDateFromAmpacheComplete(ping.session_expire).timeInMillis)
            return ping
        } catch (exception: Exception) {
            eLog(exception)
            throw exception
        }
    }

    suspend fun ping(): Boolean {
        return try {
            val ping = ampacheAuthSource.ping()
            ping.error.errorCode == 0
        } catch (exception: Exception) {
            eLog(exception)
            false
        }
    }

    suspend fun <T> reconnect(request: suspend () -> T): T {
        if (!authPersistence.hasConnectionInfo()) {
            throw SessionExpiredException("Can't reconnect")
        } else {
            authPersistence.revokeAuthToken()
            return if (authPersistence.user.secret.isNotBlank() && authPersistence.password.secret.isNotBlank()) {
                reconnectByUsernamePassword(request)
            } else {
                throw SessionExpiredException("Can't reconnect")
            }
        }
    }

    private suspend fun <T> reconnectByUsernamePassword(request: suspend () -> T): T {
        reconnectByUserPassword++
        val auth = authenticate(authPersistence.user.secret, authPersistence.password.secret)
        return if (auth.error.errorCode == 0) {
            saveData(auth)
            request()
        } else {
            throw SessionExpiredException("Can't reconnect")
        }
    }

    private fun saveData(ampacheData: AmpacheStatus) {
        saveDbCount(ampacheData)
        saveServerDates(ampacheData)
    }

    private fun saveDbCount(ampacheStatus: AmpacheStatus) {
        val edit = sharedPreferences.edit()
        edit.putInt(COUNT_SONGS, ampacheStatus.songs)
        edit.putInt(COUNT_ALBUMS, ampacheStatus.albums)
        edit.putInt(COUNT_ARTIST, ampacheStatus.artists)
        edit.putInt(COUNT_PLAYLIST, ampacheStatus.playlists)
        edit.putInt(COUNT_GENRES, ampacheStatus.playlists)
        edit.apply()
    }

    private fun saveServerDates(ampacheStatus: AmpacheStatus) {
        val edit = sharedPreferences.edit()
        edit.putLong(SERVER_ADD, TimeOperations.getDateFromAmpacheComplete(ampacheStatus.add).timeInMillis)
        edit.putLong(SERVER_UPDATE, TimeOperations.getDateFromAmpacheComplete(ampacheStatus.update).timeInMillis)
        edit.putLong(SERVER_CLEAN, TimeOperations.getDateFromAmpacheComplete(ampacheStatus.clean).timeInMillis)
        edit.apply()
    }

    enum class ConnectionStatus {
        WRONG_SERVER_URL,
        WRONG_ID_PAIR,
        CONNEXION,
        CONNECTED
    }
}
