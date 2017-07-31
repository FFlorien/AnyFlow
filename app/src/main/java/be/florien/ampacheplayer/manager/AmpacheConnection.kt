package be.florien.ampacheplayer.manager

import android.content.Context
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.business.ampache.*
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
        var ampacheApi: AmpacheApi,
        var authManager: AuthManager,
        var context: Context) {

    init {
        Timber.tag(this.javaClass.simpleName)
    }

    /**
     * API calls
     */
    fun authenticate(user: String, password: String): Observable<AmpacheAuthentication> {
        val encoder = MessageDigest.getInstance("SHA-256")
        val time = (Date().time / 1000).toString()
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
                .doOnError { Timber.e("Error while authenticating", it) }
    }

    fun ping(authToken: String = authManager.authToken): Observable<AmpachePing> {
        return ampacheApi
                .ping(auth = authToken)
                .doOnNext { result -> authManager.extendsSession(result.sessionExpire) }
                .doOnError { Timber.e("Error while ping", it) }
    }

    fun <T> reconnect(request: Observable<T>): Observable<T> {
        if (!authManager.hasConnectionInfo()) {
            return Observable.error { throw SessionExpiredException("Can't reconnect") }
        } else {
            if (authManager.authToken.isNotBlank()) {
                return ping(authManager.authToken)
                        .flatMap {
                            if (it.error.code == 0) {
                                request
                            } else {
                                authenticate(authManager.user, authManager.password)
                                        .flatMap {
                                            if (it.error.code == 0) {
                                                request
                                            } else {
                                                throw SessionExpiredException("Can't reconnect")
                                            }
                                        }
                            }
                        }
            } else if (authManager.user.isNotBlank() && authManager.password.isNotBlank()) {
                return authenticate(authManager.user, authManager.password)
                        .flatMap {
                            if (it.error.code == 0) {
                                request
                            } else {
                                throw SessionExpiredException("Can't reconnect")
                            }
                        }
            } else {
                return Observable.error { throw SessionExpiredException("Can't reconnect") }
            }
        }
    }

    fun getSongs(from: String = "1970-01-01"): Observable<AmpacheSongList> = ampacheApi.getSongs(auth = authManager.authToken, update = from)

    fun getArtists(from: String = "1970-01-01"): Observable<AmpacheArtistList> = ampacheApi.getArtists(auth = authManager.authToken, update = from)

    fun getAlbums(from: String = "1970-01-01"): Observable<AmpacheAlbumList> = ampacheApi.getAlbums(auth = authManager.authToken, update = from)

    fun getTags(from: String = "1970-01-01"): Observable<AmpacheTagList> = ampacheApi.getTags(auth = authManager.authToken, update = from)

    fun getPlaylists(from: String = "1970-01-01"): Observable<AmpachePlayListList> = ampacheApi.getPlaylists(auth = authManager.authToken, update = from)

    fun getSong(uid: Long): Observable<AmpacheSongList> = ampacheApi.getSong(auth = authManager.authToken, uid = uid)

    private fun binToHex(data: ByteArray): String = String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
}
