package be.florien.anyflow.data.server

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.UserComponentContainer
import be.florien.anyflow.data.TimeOperations
import be.florien.anyflow.data.server.exception.NotAnAmpacheUrlException
import be.florien.anyflow.data.server.exception.SessionExpiredException
import be.florien.anyflow.data.server.exception.WrongFormatServerUrlException
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.data.server.model.*
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.extension.applyPutLong
import be.florien.anyflow.extension.eLog
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.HttpException
import java.lang.IllegalStateException
import java.math.BigInteger
import java.security.MessageDigest
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
        private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val OFFSET_SONG = "songOffset"
        private const val OFFSET_ARTIST = "artistOffset"
        private const val OFFSET_ALBUM = "albumOffset"
        private const val OFFSET_TAG = "tagOffset"
        private const val OFFSET_PLAYLIST = "playlistOffset"
        private const val OFFSET_PLAYLIST_SONGS = "playlistSongOffset"
        private const val RECONNECT_LIMIT = 3
        private const val COUNT_SONGS = "SONGS_COUNT"
        private const val COUNT_ALBUMS = "ALBUMS_COUNT"
        private const val COUNT_ARTIST = "ARTIST_COUNT"
        private const val COUNT_PLAYLIST = "PLAYLIST_COUNT"
    }

    private var ampacheApi: AmpacheApi = AmpacheApiDisconnected()

    private val oldestDateForRefresh = TimeOperations.getDateFromMillis(0L)

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

    val connectionStatusUpdater = MutableLiveData(ConnectionStatus.CONNEXION)

    val songsPercentageUpdater = MutableLiveData(0)
    val artistsPercentageUpdater = MutableLiveData(0)
    val albumsPercentageUpdater = MutableLiveData(0)
    val playlistsPercentageUpdater = MutableLiveData(0)

    /**
     * Ampache connection handling
     */

    fun openConnection(serverUrl: String) {
        val url = try {
            serverUrl.toHttpUrl()
        } catch (exception: IllegalArgumentException) {
            throw WrongFormatServerUrlException(
                    "The provided url was not correctly formed",
                    exception
            )
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
        val time = (TimeOperations.getCurrentDate().timeInMillis / 1000).toString()
        val encoder = MessageDigest.getInstance("SHA-256")
        encoder.reset()
        val passwordEncoded = binToHex(encoder.digest(password.toByteArray())).lowercase(Locale.ROOT)
        encoder.reset()
        val auth = binToHex(encoder.digest((time + passwordEncoded).toByteArray())).lowercase(Locale.ROOT)
        connectionStatusUpdater.postValue(ConnectionStatus.CONNEXION)
        try {
            val authentication = ampacheApi.authenticate(user = user, auth = auth, time = time)
            when (authentication.error.code) {
                401 -> {
                    connectionStatusUpdater.postValue(ConnectionStatus.WRONG_ID_PAIR)
                    throw WrongIdentificationPairException(authentication.error.error_text)
                }
                0 -> {
                    authPersistence.saveConnectionInfo(
                            user,
                            password,
                            authentication.auth,
                            TimeOperations.getDateFromAmpacheComplete(authentication.session_expire).timeInMillis
                    )
                    saveDbCount(authentication.songs, authentication.albums, authentication.artists, authentication.playlists)
                }
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
        if (authToken.isBlank()) {
            throw IllegalArgumentException("No token available !")
        }
        try {
            connectionStatusUpdater.postValue(ConnectionStatus.CONNEXION)
            val ping = ampacheApi.ping(auth = authToken)
            if (ping.session_expire.isEmpty()) {
                return ping
            }

            saveDbCount(ping.songs, ping.albums, ping.artists, ping.playlists)
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
            saveDbCount(pingResponse.songs, pingResponse.albums, pingResponse.artists, pingResponse.playlists)
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
        val authentication = authenticate(authPersistence.user.secret, authPersistence.password.secret)
        return if (authentication.error.code == 0) {
            saveDbCount(authentication.songs, authentication.albums, authentication.artists, authentication.playlists)
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
        edit.apply()
    }

    /**
     * API calls : data
     */

    suspend fun getSongs(from: Calendar = oldestDateForRefresh): List<AmpacheSong>? {
        try {
            val songList = ampacheApi.getSongs(
                    auth = authPersistence.authToken.secret,
                    add = TimeOperations.getAmpacheGetFormatted(from),
                    limit = itemLimit,
                    offset = songOffset
            )
            val totalSongs = sharedPreferences.getInt(COUNT_SONGS, 1)
            return if (songList.isEmpty()) {
                songsPercentageUpdater.postValue(-1)
                sharedPreferences.edit().remove(OFFSET_SONG).apply()
                null
            } else {
                val percentage = (songOffset * 100) / totalSongs
                songsPercentageUpdater.postValue(percentage)
                songOffset += songList.size
                sharedPreferences.applyPutLong(OFFSET_SONG, songOffset.toLong())
                songList
            }
        } catch (ex: Exception) {
            eLog(ex)
            throw ex
        }
    }

    suspend fun getArtists(from: Calendar = oldestDateForRefresh): List<AmpacheArtist>? {
        try {
            val artistList = ampacheApi.getArtists(
                    auth = authPersistence.authToken.secret,
                    add = TimeOperations.getAmpacheGetFormatted(from),
                    limit = itemLimit,
                    offset = artistOffset
            )
            val totalArtists = sharedPreferences.getInt(COUNT_ARTIST, 1)

            if (!artistList.isSuccessful) {
                val errorBody = artistList.errorBody()
                if (errorBody != null) {
                    val readValue = ObjectMapper().readValue(errorBody.byteStream(), AmpacheErrorObject::class.java)
                    throw IllegalStateException(readValue.error.error_text)
                }
            }

            val body = artistList.body() ?: throw IllegalArgumentException("No list of artist in the response")
            return if (body.isEmpty()) {
                artistsPercentageUpdater.postValue(-1)
                sharedPreferences.edit().remove(OFFSET_ARTIST).apply()
                null
            } else {
                val percentage = (artistOffset * 100) / totalArtists
                artistsPercentageUpdater.postValue(percentage)
                artistOffset += body.size
                sharedPreferences.applyPutLong(OFFSET_ARTIST, artistOffset.toLong())
                body
            }
        } catch (ex: Exception) {
            eLog(ex)
            throw ex
        }
    }

    suspend fun getAlbums(from: Calendar = oldestDateForRefresh): List<AmpacheAlbum>? {
        try {
            val albumList = ampacheApi.getAlbums(
                    auth = authPersistence.authToken.secret,
                    add = TimeOperations.getAmpacheGetFormatted(from),
                    limit = itemLimit,
                    offset = albumOffset
            )
            val totalAlbums = sharedPreferences.getInt(COUNT_ALBUMS, 1)
            return if (albumList.isEmpty()) {
                albumsPercentageUpdater.postValue(-1)
                sharedPreferences.edit().remove(OFFSET_ALBUM).apply()
                null
            } else {
                val percentage = (albumOffset * 100) / totalAlbums
                albumsPercentageUpdater.postValue(percentage)
                albumOffset += albumList.size
                sharedPreferences.applyPutLong(OFFSET_ALBUM, albumOffset.toLong())
                albumList
            }
        } catch (ex: java.lang.Exception) {
            eLog(ex)
            throw ex
        }
    }

    suspend fun getPlaylists(): List<AmpachePlayList>? { // todo these are the new playlists
        val playlistList = ampacheApi.getPlaylists(
                auth = authPersistence.authToken.secret,
                limit = itemLimit,
                offset = playlistOffset
        )
        val totalPlaylists = sharedPreferences.getInt(COUNT_PLAYLIST, 1)
        return if (playlistList.isEmpty()) {
            playlistsPercentageUpdater.postValue(-1)
            sharedPreferences.edit().remove(OFFSET_PLAYLIST).apply()
            null
        } else {
            val percentage = (playlistOffset * 100) / totalPlaylists
            playlistsPercentageUpdater.postValue(percentage)
            playlistOffset += playlistList.size
            sharedPreferences.applyPutLong(OFFSET_PLAYLIST, playlistOffset.toLong())
            playlistList
        }
    }

    suspend fun getTags(from: Calendar = oldestDateForRefresh): List<AmpacheTag>? {
        val tagList = ampacheApi.getTags(
                auth = authPersistence.authToken.secret,
                add = TimeOperations.getAmpacheGetFormatted(from),
                limit = itemLimit,
                offset = tagOffset
        )
        return if (tagList.isEmpty()) {
            sharedPreferences.edit().remove(OFFSET_TAG).apply()
            null
        } else {
            tagOffset += tagList.size
            sharedPreferences.applyPutLong(OFFSET_TAG, tagOffset.toLong())
            tagList
        }
    }

    suspend fun getPlaylistsSongs(playlistToQuery: Long): List<AmpacheSongId>? {
        val prefName = OFFSET_PLAYLIST_SONGS + playlistToQuery
        var currentPlaylistOffset = sharedPreferences.getLong(prefName, 0).toInt()
        val playlistSongList = ampacheApi.getPlaylistSongs(
                auth = authPersistence.authToken.secret,
                filter = playlistToQuery.toString(),
                limit = itemLimit,
                offset = currentPlaylistOffset
        )
        return if (playlistSongList.isEmpty()) {
            sharedPreferences.edit().remove(prefName).apply()
            null
        } else {
            currentPlaylistOffset += playlistSongList.size
            sharedPreferences.applyPutLong(prefName, currentPlaylistOffset.toLong())
            playlistSongList
        }
    }

    suspend fun createPlaylist(name: String) {
        ampacheApi.createPlaylist(auth = authPersistence.authToken.secret, name = name)
    }

    suspend fun addSongToPlaylist(songId: Long, playlistId: Long) {
        ampacheApi.addToPlaylist(auth = authPersistence.authToken.secret, filter = playlistId, songId = songId)
    }

    fun getSongUrl(id: Long): String {
        val serverUrl = authPersistence.serverUrl.secret
        val token = authPersistence.authToken.secret
        return "${serverUrl}play/index.php?ssid=$token&oid=$id"
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
