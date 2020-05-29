package be.florien.anyflow.data.server

import android.content.SharedPreferences
import be.florien.anyflow.UserComponentContainer
import be.florien.anyflow.data.server.exception.NotAnAmpacheUrlException
import be.florien.anyflow.data.server.exception.SessionExpiredException
import be.florien.anyflow.data.server.exception.WrongFormatServerUrlException
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.data.server.model.*
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.extension.applyPutLong
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.feature.MutableValueLiveData
import okhttp3.HttpUrl
import retrofit2.HttpException
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for the ampache API server-side
 */
@Singleton
open class AmpacheConnection
@Inject constructor(
        private var authPersistence: AuthPersistence,
        private var userComponentContainer: UserComponentContainer,
        private val sharedPreferences: SharedPreferences) {
    companion object {
        private const val OFFSET_SONG = "songOffset"
        private const val OFFSET_ARTIST = "artistOffset"
        private const val OFFSET_ALBUM = "albumOffset"
        private const val OFFSET_TAG = "tagOffset"
        private const val OFFSET_PLAYLIST = "playlistOffset"
        private const val RECONNECT_LIMIT = 3
    }

    private var ampacheApi: AmpacheApi = AmpacheApiDisconnected()

    private val oldestDateForRefresh = Calendar.getInstance().apply { timeInMillis = 0L }
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val itemLimit: Int = 150
    private var songOffset: Int = sharedPreferences.getLong(OFFSET_SONG, 0).toInt()
    private var artistOffset: Int = sharedPreferences.getLong(OFFSET_ARTIST, 0).toInt()
    private var albumOffset: Int = sharedPreferences.getLong(OFFSET_ALBUM, 0).toInt()
    private var tagOffset: Int = sharedPreferences.getLong(OFFSET_TAG, 0).toInt()
    private var playlistOffset: Int = sharedPreferences.getLong(OFFSET_PLAYLIST, 0).toInt()
    private var reconnectByPing = 0
    private var reconnectByUserPassword = 0

    private var userComponent
        get() = userComponentContainer.userComponent
        set(value) {
            userComponentContainer.userComponent = value
        }

    val connectionStatusUpdater = MutableValueLiveData(ConnectionStatus.CONNEXION)

    val songsPercentageUpdater = MutableValueLiveData(-1)
    val artistsPercentageUpdater = MutableValueLiveData(-1)
    val albumsPercentageUpdater = MutableValueLiveData(-1)

    /**
     * Ampache connection handling
     */

    fun openConnection(serverUrl: String) {
        val url = try {
            HttpUrl.get(serverUrl)
        } catch (exception: IllegalArgumentException) {
            throw WrongFormatServerUrlException("The provided url was not correctly formed", exception)
        }
        ampacheApi = userComponentContainer.createUserScopeForServer(url.toString())
        authPersistence.saveServerInfo(url.toString())
    }

    fun ensureConnection() {
        if (userComponent == null) {
            val savedServerUrl = authPersistence.serverUrl.secret
            if (savedServerUrl.isNotBlank()) {
                openConnection(savedServerUrl)
            }
        }
    }

    fun resetReconnectionCount() {
        reconnectByUserPassword = 0
    }

    /**
     * API calls : connection
     */
    suspend fun authenticate(user: String, password: String): AmpacheAuthentication {
        val time = (Date().time / 1000).toString()
        val encoder = MessageDigest.getInstance("SHA-256")
        encoder.reset()
        val passwordEncoded = binToHex(encoder.digest(password.toByteArray())).toLowerCase(Locale.ROOT)
        encoder.reset()
        val auth = binToHex(encoder.digest((time + passwordEncoded).toByteArray())).toLowerCase(Locale.ROOT)
        connectionStatusUpdater.postValue(ConnectionStatus.CONNEXION)
        try {
            val authentication = ampacheApi.authenticate(user = user, auth = auth, time = time)
            when (authentication.error.code) {
                401 -> {
                    connectionStatusUpdater.postValue(ConnectionStatus.WRONG_ID_PAIR)
                    throw WrongIdentificationPairException(authentication.error.errorText)
                }
                0 -> authPersistence.saveConnectionInfo(user, password, authentication.auth, authentication.sessionExpire)
            }
            connectionStatusUpdater.postValue(ConnectionStatus.CONNECTED)
            return authentication
        } catch (exception: HttpException) {
            connectionStatusUpdater.postValue(ConnectionStatus.WRONG_SERVER_URL)
            ampacheApi = AmpacheApiDisconnected()
            throw NotAnAmpacheUrlException("The ampache server couldn't be found at provided url")
        } catch (exception: Exception) {
            eLog(exception, "Unknown error while trying to login")
            throw exception
        }
    }

    suspend fun ping(authToken: String = authPersistence.authToken.secret): AmpachePing {
        connectionStatusUpdater.postValue(ConnectionStatus.CONNEXION)
        val ping = ampacheApi.ping(auth = authToken)
        authPersistence.setNewAuthExpiration(ping.sessionExpire)
        connectionStatusUpdater.postValue(ConnectionStatus.CONNECTED)
        return ping
    }

    suspend fun <T> reconnect(request: suspend () -> T): T {
        if (!authPersistence.hasConnectionInfo()) {
            throw SessionExpiredException("Can't reconnect")
        } else {
            if (reconnectByPing >= RECONNECT_LIMIT) {
                reconnectByPing = 0
                authPersistence.revokeAuthToken()
            }
            return if (authPersistence.authToken.secret.isNotBlank()) {
                reconnectByPing(request)
            } else if (authPersistence.user.secret.isNotBlank() && authPersistence.password.secret.isNotBlank()) {
                reconnectByUsernamePassword(request)
            } else {
                throw SessionExpiredException("Can't reconnect")
            }
        }
    }

    private suspend fun <T> reconnectByPing(request: suspend () -> T): T {
        reconnectByPing++
        val pingResponse = ping(authPersistence.authToken.secret)
        return if (pingResponse.error.code == 0) {
            request()
        } else {
            val authResponse = authenticate(authPersistence.user.secret, authPersistence.password.secret)
            if (authResponse.error.code == 0) {
                request()
            } else {
                throw SessionExpiredException("Can't reconnect")
            }
        }
    }

    private suspend fun <T> reconnectByUsernamePassword(request: suspend () -> T): T {
        reconnectByUserPassword++
        val it = authenticate(authPersistence.user.secret, authPersistence.password.secret)
        return if (it.error.code == 0) {
            request()
        } else {
            throw SessionExpiredException("Can't reconnect")
        }
    }

    /**
     * API calls : data
     */

    suspend fun getSongs(from: Calendar = oldestDateForRefresh): AmpacheSongList? {
        val songList = ampacheApi.getSongs(auth = authPersistence.authToken.secret, add = dateFormatter.format(from.time), limit = itemLimit, offset = songOffset)
        return if (songList.songs.isEmpty()) {
            songsPercentageUpdater.postValue(-1)
            sharedPreferences.edit().remove(OFFSET_SONG).apply()
            null
        } else {
            val percentage = (songOffset * 100) / songList.total_count
            songsPercentageUpdater.postValue(percentage)
            sharedPreferences.applyPutLong(OFFSET_SONG, songOffset.toLong())
            songOffset += itemLimit
            songList
        }
    }

    suspend fun getArtists(from: Calendar = oldestDateForRefresh): AmpacheArtistList? {
        val artistList = ampacheApi.getArtists(auth = authPersistence.authToken.secret, add = dateFormatter.format(from.time), limit = itemLimit, offset = artistOffset)

        return if (artistList.artists.isEmpty()) {
            artistsPercentageUpdater.postValue(-1)
            sharedPreferences.edit().remove(OFFSET_ARTIST).apply()
            null
        } else {
            val percentage = (artistOffset * 100) / artistList.total_count
            artistsPercentageUpdater.postValue(percentage)
            sharedPreferences.applyPutLong(OFFSET_ARTIST, artistOffset.toLong())
            artistOffset += itemLimit
            artistList
        }
    }

    suspend fun getAlbums(from: Calendar = oldestDateForRefresh): AmpacheAlbumList? {
        val albumList = ampacheApi.getAlbums(auth = authPersistence.authToken.secret, add = dateFormatter.format(from.time), limit = itemLimit, offset = albumOffset)
        return if (albumList.albums.isEmpty()) {
            albumsPercentageUpdater.postValue(-1)
            sharedPreferences.edit().remove(OFFSET_ALBUM).apply()
            null
        } else {
            val percentage = (albumOffset * 100) / albumList.total_count
            albumsPercentageUpdater.postValue(percentage)
            sharedPreferences.applyPutLong(OFFSET_ALBUM, albumOffset.toLong())
            albumOffset += itemLimit
            albumList
        }
    }

    suspend fun getTags(from: Calendar = oldestDateForRefresh): AmpacheTagList? {
        val tagList = ampacheApi.getTags(auth = authPersistence.authToken.secret, add = dateFormatter.format(from.time), limit = itemLimit, offset = tagOffset)
        return if (tagList.tags.isEmpty()) {
            sharedPreferences.edit().remove(OFFSET_TAG).apply()
            null
        } else {
            sharedPreferences.applyPutLong(OFFSET_TAG, tagOffset.toLong())
            tagOffset += itemLimit
            tagList
        }
    }

    suspend fun getPlaylists(from: Calendar = oldestDateForRefresh): AmpachePlayListList? {
        val playlistList = ampacheApi.getPlaylists(auth = authPersistence.authToken.secret, add = dateFormatter.format(from.time), limit = itemLimit, offset = playlistOffset)
        return if (playlistList.playlists.isEmpty()) {
            sharedPreferences.edit().remove(OFFSET_PLAYLIST).apply()
            null
        } else {
            sharedPreferences.applyPutLong(OFFSET_PLAYLIST, playlistOffset.toLong())
            playlistOffset += itemLimit
            playlistList
        }
    }

    fun getSongUrl(url: String): String {
        val ssidStart = url.indexOf("ssid=") + 5
        return url.replaceRange(ssidStart, url.indexOf('&', ssidStart), authPersistence.authToken.secret)
    }

    private fun binToHex(data: ByteArray): String = String.format("%0" + data.size * 2 + "X", BigInteger(1, data))

    enum class ConnectionStatus {
        WRONG_SERVER_URL,
        WRONG_ID_PAIR,
        CONNEXION,
        CONNECTED
    }
}
