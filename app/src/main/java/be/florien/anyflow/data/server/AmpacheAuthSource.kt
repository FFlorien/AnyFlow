package be.florien.anyflow.data.server

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.TimeOperations
import be.florien.anyflow.data.server.exception.NotAnAmpacheUrlException
import be.florien.anyflow.data.server.exception.SessionExpiredException
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.data.server.model.AmpacheAuthentication
import be.florien.anyflow.data.server.model.AmpachePing
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ServerScope
import retrofit2.HttpException
import retrofit2.Retrofit
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject

/**
 * Manager for the ampache API server-side
 */
@ServerScope
open class AmpacheAuthSource
@Inject constructor(
    retrofit: Retrofit,
    private var authPersistence: AuthPersistence,
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

    private var ampacheAuthApi: AmpacheAuthApi = retrofit.create(AmpacheAuthApi::class.java)

    private var reconnectByUserPassword = 0

    val connectionStatusUpdater = MutableLiveData(ConnectionStatus.CONNEXION)

    /**
     * API calls : connection
     */
    suspend fun authenticate(user: String, password: String): AmpacheAuthentication {
        val time = (TimeOperations.getCurrentDate().timeInMillis / 1000).toString()
        val encoder = MessageDigest.getInstance("SHA-256")
        encoder.reset()
        val passwordEncoded =
            binToHex(encoder.digest(password.toByteArray())).lowercase(Locale.ROOT)
        encoder.reset()
        val auth =
            binToHex(encoder.digest((time + passwordEncoded).toByteArray())).lowercase(Locale.ROOT)
        connectionStatusUpdater.postValue(ConnectionStatus.CONNEXION)
        try {
            val authentication = ampacheAuthApi.authenticate(user = user, auth = auth, time = time)
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
                    saveDbCount(
                        authentication.songs,
                        authentication.albums,
                        authentication.artists,
                        authentication.playlists
                    )
                    saveServerDates(
                        TimeOperations.getDateFromAmpacheComplete(authentication.add),
                        TimeOperations.getDateFromAmpacheComplete(authentication.update),
                        TimeOperations.getDateFromAmpacheComplete(authentication.clean)
                    )
                }
            }
            connectionStatusUpdater.postValue(ConnectionStatus.CONNECTED)
            return authentication
        } catch (exception: HttpException) {
            connectionStatusUpdater.postValue(ConnectionStatus.WRONG_SERVER_URL)
            ampacheAuthApi = AmpacheApiDisconnected()
            throw NotAnAmpacheUrlException("The ampache server couldn't be found at provided url")
        } catch (exception: Exception) {
            eLog(exception, "Unknown error while trying to login")
            throw exception
        }
    }

    suspend fun ping(): AmpachePing {
        val authToken: String = authPersistence.authToken.secret
        if (authToken.isBlank()) {
            throw IllegalArgumentException("No token available !")
        }
        try {
            connectionStatusUpdater.postValue(ConnectionStatus.CONNEXION)
            val ping = ampacheAuthApi.ping(auth = authToken)
            if (ping.session_expire.isEmpty()) {
                return ping
            }

            saveDbCount(ping.songs, ping.albums, ping.artists, ping.playlists)
            saveServerDates(
                TimeOperations.getDateFromAmpacheComplete(ping.add),
                TimeOperations.getDateFromAmpacheComplete(ping.update),
                TimeOperations.getDateFromAmpacheComplete(ping.clean)
            )
            authPersistence.setNewAuthExpiration(TimeOperations.getDateFromAmpacheComplete(ping.session_expire).timeInMillis)
            connectionStatusUpdater.postValue(ConnectionStatus.CONNECTED)
            return ping
        } catch (exception: Exception) {
            eLog(exception)
            throw exception
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
            saveDbCount(auth.songs, auth.albums, auth.artists, auth.playlists)
            request()
        } else {
            throw SessionExpiredException("Can't reconnect")
        }
    }

    private fun saveDbCount(songs: Int, albums: Int, artists: Int, playlists: Int) {
        val edit = sharedPreferences.edit()
        edit.putInt(COUNT_SONGS, songs)
        edit.putInt(COUNT_ALBUMS, albums)
        edit.putInt(COUNT_ARTIST, artists)
        edit.putInt(COUNT_PLAYLIST, playlists)
        edit.putInt(COUNT_GENRES, playlists)
        edit.apply()
    }

    private fun saveServerDates(add: Calendar, update: Calendar, clean: Calendar) {
        val edit = sharedPreferences.edit()
        edit.putLong(SERVER_ADD, add.timeInMillis)
        edit.putLong(SERVER_UPDATE, update.timeInMillis)
        edit.putLong(SERVER_CLEAN, clean.timeInMillis)
        edit.apply()
    }

    private fun binToHex(data: ByteArray): String =
        String.format("%0" + data.size * 2 + "X", BigInteger(1, data))

    enum class ConnectionStatus {
        WRONG_SERVER_URL,
        WRONG_ID_PAIR,
        CONNEXION,
        CONNECTED
    }
}
