package be.florien.ampacheplayer.manager

import android.content.Context
import be.florien.ampacheplayer.business.ampache.*
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import io.reactivex.Observable
import timber.log.Timber
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject

/**
 * Manager for the ampache API server-side
 */
class AmpacheConnection
@Inject constructor(
        private var ampacheApi: AmpacheApi,
        private var authManager: AuthManager,
        var context: Context) {

    init {
        Timber.tag(this.javaClass.simpleName)
    }

    /**
     * API calls
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
                        0 -> {
                            authManager.authenticate(user, password, result.auth, result.sessionExpire)
                        }
                    }
                }
                .doOnError { Timber.e(it, "Error while authenticating") }
    }

    fun ping(authToken: String = authManager.authToken.first): Observable<AmpachePing> {
        return ampacheApi
                .ping(auth = authToken)
                .doOnNext { result -> authManager.setNewAuthExpiration(result.sessionExpire) }
                .doOnError { Timber.e(it, "Error while ping") }
    }

    fun <T> reconnect(request: Observable<T>): Observable<T> {
        if (!authManager.hasConnectionInfo()) {
            return Observable.error { throw SessionExpiredException("Can't reconnect") }
        } else {
            return if (authManager.authToken.first.isNotBlank()) {
                ping(authManager.authToken.first).flatMap { //todo it's already too late by now, we should ping more often
                    if (it.error.code == 0) {
                        request
                    } else {
                        authenticate(authManager.user.first, authManager.password.first).flatMap {
                            if (it.error.code == 0) {
                                request
                            } else {
                                throw SessionExpiredException("Can't reconnect")
                            }
                        }
                    }
                }
            } else if (authManager.user.first.isNotBlank() && authManager.password.first.isNotBlank()) {
                authenticate(authManager.user.first, authManager.password.first).flatMap {
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

    fun getSongs(from: String = "1970-01-01"): Observable<AmpacheSongList> = ampacheApi.getSongs(auth = authManager.authToken.first, update = from)

    fun getArtists(from: String = "1970-01-01"): Observable<AmpacheArtistList> = ampacheApi.getArtists(auth = authManager.authToken.first, update = from)

    fun getAlbums(from: String = "1970-01-01"): Observable<AmpacheAlbumList> = ampacheApi.getAlbums(auth = authManager.authToken.first, update = from)

    fun getTags(from: String = "1970-01-01"): Observable<AmpacheTagList> = ampacheApi.getTags(auth = authManager.authToken.first, update = from)

    fun getPlaylists(from: String = "1970-01-01"): Observable<AmpachePlayListList> = ampacheApi.getPlaylists(auth = authManager.authToken.first, update = from)

    fun getSong(uid: Long): Observable<AmpacheSongList> = ampacheApi.getSong(auth = authManager.authToken.first, uid = uid)

    private fun binToHex(data: ByteArray): String = String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
}
