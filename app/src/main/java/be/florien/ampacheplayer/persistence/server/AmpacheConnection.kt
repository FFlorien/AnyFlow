package be.florien.ampacheplayer.persistence.server

import android.content.Context
import android.content.SharedPreferences
import be.florien.ampacheplayer.AmpacheApp
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.extension.applyPutLong
import be.florien.ampacheplayer.persistence.local.model.Song
import be.florien.ampacheplayer.persistence.server.model.*
import be.florien.ampacheplayer.user.AuthPersistence
import io.reactivex.Observable
import timber.log.Timber
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
        private var context: Context,
        private val sharedPreferences: SharedPreferences) {
    companion object {
        private const val OFFSET_SONG = "songOffset"
        private const val OFFSET_ARTIST = "artistOffset"
        private const val OFFSET_ALBUM = "albumOffset"
        private const val OFFSET_TAG = "tagOffset"
        private const val OFFSET_PLAYLIST = "playlistOffset"
    }

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
        get() = (context.applicationContext as AmpacheApp).userComponent
        set(value) {
            (context.applicationContext as AmpacheApp).userComponent = value
        }

    init {
        Timber.tag(this.javaClass.simpleName)
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
        ampacheApi = (context.applicationContext as AmpacheApp).createUserScopeForServer(correctedUrl)
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
                .doOnError { Timber.e(it, "Error while authenticating") }
    }

    //todo it's already too late by now, we should ping more often
    fun ping(authToken: String = authPersistence.authToken.first): Observable<AmpachePing> =
            ampacheApi
                    .ping(auth = authToken)
                    .doOnNext { result -> authPersistence.setNewAuthExpiration(result.sessionExpire) }
                    .doOnError { Timber.e(it, "Error while ping") }

    fun <T> reconnect(request: Observable<T>): Observable<T> {//todo retrieve serverurl from authpersistence if NoServerException. Or look before
        if (!authPersistence.hasConnectionInfo()) {
            return Observable.error { throw SessionExpiredException("Can't reconnect") }
        } else {
            return if (authPersistence.authToken.first.isNotBlank()) {
                ping(authPersistence.authToken.first).flatMap {
                    if (it.error.code == 0) {
                        request
                    } else {
                        authenticate(authPersistence.user.first, authPersistence.password.first).flatMap {
                            if (it.error.code == 0) {
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
                .subscribe()
    }

    fun getSongUrl(song: Song): String {
        val ssidStart = song.url.indexOf("ssid=") + 5
        return song.url.replaceRange(ssidStart, song.url.indexOf('&', ssidStart), authPersistence.authToken.first)
    }

    private fun binToHex(data: ByteArray): String = String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
}
