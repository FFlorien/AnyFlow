package be.florien.anyflow.persistence.server

import android.content.Context
import android.content.SharedPreferences
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.exception.SessionExpiredException
import be.florien.anyflow.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.applyPutLong
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.persistence.local.model.Song
import be.florien.anyflow.persistence.server.model.*
import be.florien.anyflow.user.AuthPersistence
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger
import java.net.ConnectException
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
    }

    private val subscriptions: CompositeDisposable = CompositeDisposable() //todo

    private var ampacheApi: AmpacheApi = AmpacheApiDisconnected()

    private val oldestDateForRefresh = Calendar.getInstance().apply { timeInMillis = 0L }
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val itemLimit: Int = 50
    private var songOffset: Int = sharedPreferences.getLong(OFFSET_SONG, 0).toInt()
    private var artistOffset: Int = sharedPreferences.getLong(OFFSET_ARTIST, 0).toInt()
    private var albumOffset: Int = sharedPreferences.getLong(OFFSET_ALBUM, 0).toInt()
    private var tagOffset: Int = sharedPreferences.getLong(OFFSET_TAG, 0).toInt()
    private var playlistOffset: Int = sharedPreferences.getLong(OFFSET_PLAYLIST, 0).toInt()

    private var userComponent
        get() = (context.applicationContext as AnyFlowApp).userComponent
        set(value) {
            (context.applicationContext as AnyFlowApp).userComponent = value
        }

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
                .doOnNext { result ->
                    when (result.error.code) {
                        401 -> throw WrongIdentificationPairException(result.error.errorText)
                        0 -> authPersistence.saveConnectionInfo(user, password, result.auth, result.sessionExpire)
                    }
                }
                .doOnError { this@AmpacheConnection.eLog(it, "Error while authenticating") }
    }

    //todo it's already too late by now, we should ping more often
    fun ping(authToken: String = authPersistence.authToken.first): Observable<AmpachePing> =
            ampacheApi
                    .ping(auth = authToken)
                    .doOnNext { result -> authPersistence.setNewAuthExpiration(result.sessionExpire) }
                    .doOnError { this@AmpacheConnection.eLog(it, "Error while ping") }
                    .subscribeOn(Schedulers.io())

    fun <T> reconnect(request: Observable<T>): Observable<T> {//todo retrieve serverurl from authpersistence if NoServerException. Or look before
        if (!authPersistence.hasConnectionInfo()) {
            return Observable.error { throw SessionExpiredException("Can't reconnect") }
        } else {
            return if (authPersistence.authToken.first.isNotBlank()) {
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
        }
    }

    /**
     * API calls : data
     */

    fun getSongs(from: Calendar = oldestDateForRefresh): Observable<AmpacheSongList> = Observable.generate { emitter ->
        ampacheApi
                .getSongs(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time), limit = itemLimit, offset = songOffset)
                .doOnNext {
                    if (it.songs.isEmpty()) {
                        emitter.onComplete()
                        sharedPreferences.edit().remove(OFFSET_SONG).apply()
                    } else {
                        emitter.onNext(it)
                        sharedPreferences.applyPutLong(OFFSET_SONG, songOffset.toLong())
                        songOffset += itemLimit
                    }
                }
                .doOnError {
                    this@AmpacheConnection.eLog(it, "Error while getSongs")
                    when (it) {
                        is TimeoutException -> {
                        }
                        is ConnectException -> {
                        }
                        else -> {
                        }
                        //todo inform user and let him try later

                    }
                }
                .subscribe()
    }

    fun getArtists(from: Calendar = oldestDateForRefresh): Observable<AmpacheArtistList> = Observable.generate { emitter ->
        ampacheApi.getArtists(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time), limit = itemLimit, offset = artistOffset)
                .doOnNext {
                    if (it.artists.isEmpty()) {
                        emitter.onComplete()
                        sharedPreferences.edit().remove(OFFSET_ARTIST).apply()
                    } else {
                        emitter.onNext(it)
                        sharedPreferences.applyPutLong(OFFSET_ARTIST, artistOffset.toLong())
                        artistOffset += itemLimit
                    }
                }
                .doOnError {
                    this@AmpacheConnection.eLog(it, "Error while getArtists")
                    if (it is TimeoutException) {
                        //todo inform user and let him try later
                    }
                }
                .subscribe()
    }

    fun getAlbums(from: Calendar = oldestDateForRefresh): Observable<AmpacheAlbumList> = Observable.generate { emitter ->
        ampacheApi.getAlbums(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time), limit = itemLimit, offset = albumOffset)
                .doOnNext {
                    if (it.albums.isEmpty()) {
                        emitter.onComplete()
                        sharedPreferences.edit().remove(OFFSET_ALBUM).apply()
                    } else {
                        emitter.onNext(it)
                        sharedPreferences.applyPutLong(OFFSET_ALBUM, albumOffset.toLong())
                        albumOffset += itemLimit
                    }
                }
                .doOnError {
                    this@AmpacheConnection.eLog(it, "Error while getAlbums")
                    if (it is TimeoutException) {
                        //todo inform user and let him try later
                    }
                }
                .subscribe()
    }

    fun getTags(from: Calendar = oldestDateForRefresh): Observable<AmpacheTagList> = Observable.generate { emitter ->
        ampacheApi.getTags(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time), limit = itemLimit, offset = tagOffset)
                .doOnNext {
                    if (it.tags.isEmpty()) {
                        emitter.onComplete()
                        sharedPreferences.edit().remove(OFFSET_TAG).apply()
                    } else {
                        emitter.onNext(it)
                        sharedPreferences.applyPutLong(OFFSET_TAG, tagOffset.toLong())
                        tagOffset += itemLimit
                    }
                }
                .doOnError {
                    this@AmpacheConnection.eLog(it, "Error while getTags")
                    if (it is TimeoutException) {
                        //todo inform user and let him try later
                    }
                }
                .subscribe()
    }

    fun getPlaylists(from: Calendar = oldestDateForRefresh): Observable<AmpachePlayListList> = Observable.generate { emitter ->
        ampacheApi.getPlaylists(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time), limit = itemLimit, offset = playlistOffset)
                .doOnNext {
                    if (it.playlists.isEmpty()) {
                        emitter.onComplete()
                        sharedPreferences.edit().remove(OFFSET_PLAYLIST).apply()
                    } else {
                        emitter.onNext(it)
                        sharedPreferences.applyPutLong(OFFSET_PLAYLIST, playlistOffset.toLong())
                        playlistOffset += itemLimit
                    }
                }
                .doOnError {
                    this@AmpacheConnection.eLog(it, "Error while getPlaylists")
                    if (it is TimeoutException) {
                        //todo inform user and let him try later
                    }
                }
                .subscribe()
    }

    fun getSongUrl(song: Song): String {
        val ssidStart = song.url.indexOf("ssid=") + 5
        return song.url.replaceRange(ssidStart, song.url.indexOf('&', ssidStart), authPersistence.authToken.first)
    }

    private fun binToHex(data: ByteArray): String = String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
}
