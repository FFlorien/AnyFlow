package be.florien.ampacheplayer.manager

import android.content.Context
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.model.ampache.*
import io.reactivex.Observable
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
    }

    fun ping(authToken: String = authManager.authToken): Observable<AmpachePing> {
        return ampacheApi
                .ping(auth = authToken)
                .doOnNext { result -> authManager.extendsSession(result.sessionExpire) }
    }

    fun getSongs(from: String): Observable<AmpacheSongList> = ampacheApi.getSongs(auth = authManager.authToken, update = from)

    fun getArtists(from: String): Observable<AmpacheArtistList> = ampacheApi.getArtists(auth = authManager.authToken, update = from)

    fun getAlbums(from: String): Observable<AmpacheAlbumList> = ampacheApi.getAlbums(auth = authManager.authToken, update = from)

    fun getTags(from: String): Observable<AmpacheTagList> = ampacheApi.getTags(auth = authManager.authToken, update = from)

    fun getPlaylists(from: String): Observable<AmpachePlayListList> = ampacheApi.getPlaylists(auth = authManager.authToken, update = from)

    fun getSong(uid: Long): Observable<AmpacheSongList> = ampacheApi.getSong(auth = authManager.authToken, uid = uid)

    private fun binToHex(data: ByteArray): String = String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
}
