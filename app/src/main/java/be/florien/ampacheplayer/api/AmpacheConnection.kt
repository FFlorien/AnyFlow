package be.florien.ampacheplayer.api

import android.content.Context
import be.florien.ampacheplayer.AmpacheApp
import be.florien.ampacheplayer.api.model.*
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.user.AuthPersistence
import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
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
        private var context: Context) {
    private var ampacheApi: AmpacheApi = AmpacheApiDisconnected()

    init {
        Timber.tag(this.javaClass.simpleName)
    }

    private val oldestDateForRefresh = Calendar.getInstance().apply { timeInMillis = 0L }
    private val dateFormatter = SimpleDateFormat("aaaa-MM-dd", Locale.getDefault())
    open val hasMockUpMode = false

    fun openConnection(serverUrl: String) {
        val correctedUrl = if (!serverUrl.endsWith('/')) {
            serverUrl + "/"
        } else {
            serverUrl
        }
        val applicationComponent = (context.applicationContext as AmpacheApp).applicationComponent
        ampacheApi = Retrofit
                .Builder()
                .baseUrl(correctedUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()
                .create(AmpacheApi::class.java)
        (context.applicationContext as AmpacheApp).userComponent = applicationComponent
                //todo don't generate a new one if it is the same server
                .userComponentBuilder()
                .ampacheApi(ampacheApi)
                .build()
        authPersistence.saveServerInfo(serverUrl)
    }

    fun closeConnection() {
        ampacheApi = AmpacheApiDisconnected()
        (context.applicationContext as AmpacheApp).userComponent = null
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
                        0 -> authPersistence.saveConnectionInfo(user, password, result.auth, result.sessionExpire)
                    }
                }
                .doOnError { Timber.e(it, "Error while authenticating") }
    }

    fun ping(authToken: String = authPersistence.authToken.first): Observable<AmpachePing> {
        return ampacheApi
                .ping(auth = authToken)
                .doOnNext { result -> authPersistence.setNewAuthExpiration(result.sessionExpire) }
                .doOnError { Timber.e(it, "Error while ping") }
    }

    fun <T> reconnect(request: Observable<T>): Observable<T> {//todo retrieve serverurl from authpersistence if NoServerException. Or look before
        if (!authPersistence.hasConnectionInfo()) {
            return Observable.error { throw SessionExpiredException("Can't reconnect") }
        } else {
            return if (authPersistence.authToken.first.isNotBlank()) {
                ping(authPersistence.authToken.first).flatMap {
                    //todo it's already too late by now, we should ping more often
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

    fun getSongs(from: Calendar = oldestDateForRefresh): Observable<AmpacheSongList> = ampacheApi.getSongs(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time))

    fun getArtists(from: Calendar = oldestDateForRefresh): Observable<AmpacheArtistList> = ampacheApi.getArtists(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time))

    fun getAlbums(from: Calendar = oldestDateForRefresh): Observable<AmpacheAlbumList> = ampacheApi.getAlbums(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time))

    fun getTags(from: Calendar = oldestDateForRefresh): Observable<AmpacheTagList> = ampacheApi.getTags(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time))

    fun getPlaylists(from: Calendar = oldestDateForRefresh): Observable<AmpachePlayListList> = ampacheApi.getPlaylists(auth = authPersistence.authToken.first, update = dateFormatter.format(from.time))

    fun getSong(uid: Long): Observable<AmpacheSongList> = ampacheApi.getSong(auth = authPersistence.authToken.first, uid = uid)

    private fun binToHex(data: ByteArray): String = String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
}
