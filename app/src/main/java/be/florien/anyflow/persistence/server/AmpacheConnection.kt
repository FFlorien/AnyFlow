package be.florien.anyflow.persistence.server

import android.content.Context
import android.content.SharedPreferences
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.exception.SessionExpiredException
import be.florien.anyflow.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.applyPutLong
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.persistence.server.model.*
import be.florien.anyflow.user.AuthPersistence
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for the ampache API server-side
 */
@Singleton
open class AmpacheConnection
@Inject constructor(
        private var authPersistence: AuthPersistence,
        private var context: Context,
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
        get() = (context.applicationContext as AnyFlowApp).userComponent
        set(value) {
            (context.applicationContext as AnyFlowApp).userComponent = value
        }

    private val _connectionStatusUpdater: BehaviorSubject<ConnectionStatus> = BehaviorSubject.create<ConnectionStatus>()
    val connectionStatusUpdater: Observable<ConnectionStatus> = _connectionStatusUpdater

    private val _songsPercentageUpdater = BehaviorSubject.create<Int>()
    val songsPercentageUpdater: Observable<Int> = _songsPercentageUpdater
    private val _artistsPercentageUpdater = BehaviorSubject.create<Int>()
    val artistsPercentageUpdater: Observable<Int> = _artistsPercentageUpdater
    private val _albumsPercentageUpdater = BehaviorSubject.create<Int>()
    val albumsPercentageUpdater: Observable<Int> = _albumsPercentageUpdater

    /**
     * Ampache connection handling
     */

    fun openConnection(serverUrl: String) {
        val correctedUrl = if (!serverUrl.endsWith('/')) {
            "$serverUrl/"
        } else {
            serverUrl
        }
        ampacheApi = (context.applicationContext as AnyFlowApp).createUserScopeForServer(correctedUrl)
        authPersistence.saveServerInfo(correctedUrl)
    }

    fun ensureConnection() {
        if (userComponent == null) {
            val savedServerUrl = authPersistence.serverUrl.first
            if (savedServerUrl.isNotBlank()) {
                openConnection(savedServerUrl)
            }
        }
    }

    fun resetReconnectionCount() {
        reconnectByUserPassword = 0
        reconnectByUserPassword = 0
    }

    /**
     * API calls : connection
     */
    fun authenticate(user: String, password: String): Observable<AmpacheAuthentication> {
        val time = (Date().time / 1000).toString()
        val encoder = MessageDigest.getInstance("SHA-256")
        encoder.reset()
        val passwordEncoded = binToHex(encoder.digest(password.toByteArray())).toLowerCase()
        encoder.reset()
        val auth = binToHex(encoder.digest((time + passwordEncoded).toByteArray())).toLowerCase()
        return ampacheApi
                .authenticate(user = user, auth = auth, time = time)
                .doOnSubscribe {
                    _connectionStatusUpdater.onNext(ConnectionStatus.CONNEXION)
                }
                .doOnNext { result ->
                    when (result.error.code) {
                        401 -> {
                            _connectionStatusUpdater.onNext(ConnectionStatus.WRONG_ID_PAIR)
                            throw WrongIdentificationPairException(result.error.errorText)
                        }
                        0 -> authPersistence.saveConnectionInfo(user, password, result.auth, result.sessionExpire)
                    }
                    _connectionStatusUpdater.onNext(ConnectionStatus.CONNECTED)
                }
                .doOnError { this@AmpacheConnection.eLog(it, "Error while authenticating") }
    }

    fun ping(authToken: String = authPersistence.authToken.first): Observable<AmpachePing> =
            ampacheApi
                    .ping(auth = authToken)
                    .doOnSubscribe { _connectionStatusUpdater.onNext(ConnectionStatus.CONNEXION) }
                    .doOnNext { result ->
                        authPersistence.setNewAuthExpiration(result.sessionExpire)
                        _connectionStatusUpdater.onNext(ConnectionStatus.CONNECTED)
                    }
                    .doOnError { this@AmpacheConnection.eLog(it, "Error while ping") }
                    .subscribeOn(Schedulers.io())

    fun <T> reconnect(request: Observable<T>): Observable<T> {
        if (!authPersistence.hasConnectionInfo()) {
            return Observable.error { throw SessionExpiredException("Can't reconnect") }
        } else {
            if (reconnectByPing >= RECONNECT_LIMIT) {
                reconnectByPing = 0
                authPersistence.revokeAuthToken()
            }
            val reconnectionObservable = if (authPersistence.authToken.first.isNotBlank()) {
                reconnectByPing++
                ping(authPersistence.authToken.first).flatMap { pingResponse ->
                    if (pingResponse.error.code == 0) {
                        request
                    } else {
                        authenticate(authPersistence.user.first, authPersistence.password.first).flatMap { authResponse ->
                            if (authResponse.error.code == 0) {
                                request
                            } else {
                                throw SessionExpiredException("Can't reconnect")
                            }
                        }
                    }
                }
            } else if (authPersistence.user.first.isNotBlank() && authPersistence.password.first.isNotBlank()) {
                reconnectByUserPassword++
                authenticate(authPersistence.user.first, authPersistence.password.first).flatMap {
                    if (it.error.code == 0) {
                        request
                    } else {
                        throw SessionExpiredException("Can't reconnect")
                    }
                }
            } else {
                Observable.error { throw SessionExpiredException("Can't reconnect") }
            }
            return reconnectionObservable.subscribeOn(Schedulers.io())
        }
    }

    /**
     * API calls : data
     */

    fun getSongs(from: Calendar = oldestDateForRefresh): Observable<AmpacheSongList> = Observable.generate { emitter ->
        ampacheApi
                .getSongs(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time), limit = itemLimit, offset = songOffset)
                .subscribe(
                        {
                            if (it.songs.isEmpty()) {
                                _songsPercentageUpdater.onNext(100)
                                emitter.onComplete()
                                sharedPreferences.edit().remove(OFFSET_SONG).apply()
                            } else {
                                val percentage = (songOffset * 100) / it.total_count
                                _songsPercentageUpdater.onNext(percentage)
                                emitter.onNext(it)
                                sharedPreferences.applyPutLong(OFFSET_SONG, songOffset.toLong())
                                songOffset += itemLimit
                            }
                        },
                        {
                            dispatchError(_songsPercentageUpdater, it, "getSongs")
                            emitter.onError(it)
                        })
    }

    fun getArtists(from: Calendar = oldestDateForRefresh): Observable<AmpacheArtistList> = Observable.generate { emitter ->
        ampacheApi.getArtists(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time), limit = itemLimit, offset = artistOffset)
                .subscribe(
                        {
                            if (it.artists.isEmpty()) {
                                _artistsPercentageUpdater.onNext(100)
                                emitter.onComplete()
                                sharedPreferences.edit().remove(OFFSET_ARTIST).apply()
                            } else {
                                val percentage = (artistOffset * 100) / it.total_count
                                _artistsPercentageUpdater.onNext(percentage)
                                emitter.onNext(it)
                                sharedPreferences.applyPutLong(OFFSET_ARTIST, artistOffset.toLong())
                                artistOffset += itemLimit
                            }
                        },
                        {
                            dispatchError(_artistsPercentageUpdater, it, "getArtists")
                            emitter.onError(it)
                        })
    }

    fun getAlbums(from: Calendar = oldestDateForRefresh): Observable<AmpacheAlbumList> = Observable.generate { emitter ->
        ampacheApi.getAlbums(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time), limit = itemLimit, offset = albumOffset)
                .subscribe(
                        {
                            if (it.albums.isEmpty()) {
                                _albumsPercentageUpdater.onNext(100)
                                emitter.onComplete()
                                sharedPreferences.edit().remove(OFFSET_ALBUM).apply()
                            } else {
                                val percentage = (albumOffset * 100) / it.total_count
                                _albumsPercentageUpdater.onNext(percentage)
                                emitter.onNext(it)
                                sharedPreferences.applyPutLong(OFFSET_ALBUM, albumOffset.toLong())
                                albumOffset += itemLimit
                            }
                        },
                        {
                            dispatchError(_albumsPercentageUpdater, it, "getAlbums")
                            emitter.onError(it)
                        })
    }

    fun getTags(from: Calendar = oldestDateForRefresh): Observable<AmpacheTagList> = Observable.generate { emitter ->
        ampacheApi.getTags(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time), limit = itemLimit, offset = tagOffset)
                .subscribe(
                        {
                            if (it.tags.isEmpty()) {
                                emitter.onComplete()
                                sharedPreferences.edit().remove(OFFSET_TAG).apply()
                            } else {
                                emitter.onNext(it)
                                sharedPreferences.applyPutLong(OFFSET_TAG, tagOffset.toLong())
                                tagOffset += itemLimit
                            }
                        },
                        {
                            dispatchError(null, it, "getTags")
                            emitter.onError(it)
                        })
    }

    fun getPlaylists(from: Calendar = oldestDateForRefresh): Observable<AmpachePlayListList> = Observable.generate { emitter ->
        ampacheApi.getPlaylists(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time), limit = itemLimit, offset = playlistOffset)
                .subscribe(
                        {
                            if (it.playlists.isEmpty()) {
                                emitter.onComplete()
                                sharedPreferences.edit().remove(OFFSET_PLAYLIST).apply()
                            } else {
                                emitter.onNext(it)
                                sharedPreferences.applyPutLong(OFFSET_PLAYLIST, playlistOffset.toLong())
                                playlistOffset += itemLimit
                            }
                        },
                        {
                            dispatchError(null, it, "getPlaylists")
                            emitter.onError(it)
                        })
    }

    fun getSongUrl(url: String): String {
        if (authPersistence.authToken.first.isBlank()) {
            _connectionStatusUpdater.onNext(ConnectionStatus.CONNEXION)
        }
        val ssidStart = url.indexOf("ssid=") + 5
        return url.replaceRange(ssidStart, url.indexOf('&', ssidStart), authPersistence.authToken.first)
    }

    private fun binToHex(data: ByteArray): String = String.format("%0" + data.size * 2 + "X", BigInteger(1, data))

    private fun dispatchError(updater: BehaviorSubject<Int>?, it: Throwable, action: String) {
        updater?.onNext(-1)
        this@AmpacheConnection.eLog(it, "Error while $action")
        if (it is TimeoutException) {
            _connectionStatusUpdater.onNext(ConnectionStatus.TIMEOUT)
        }
    }

    enum class ConnectionStatus {
        TIMEOUT,
        WRONG_ID_PAIR,
        CONNEXION,
        CONNECTED
    }
}
