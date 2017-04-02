package be.florien.ampacheplayer.model.manager

import be.florien.ampacheplayer.App
import be.florien.ampacheplayer.model.retrofit.AmpacheApi
import be.florien.ampacheplayer.model.server.*
import io.reactivex.Observable
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject

/**
 * Manager for the ampache API server-side
 */
class AmpacheConnection {
    /**
     * Fields
     */
    var authSession: String = ""
    @Inject lateinit var ampacheApi: AmpacheApi

    /**
     * Constructors
     */
    init {
        App.ampacheComponent.inject(this)
    }

    /**
     * API calls
     */
    fun authenticate(user: String, password: String): Observable<Authentication> {
        val encoder = MessageDigest.getInstance("SHA-256")
        val time = (Date().time / 1000).toString()
        encoder.reset()
        val passwordEncoded = binToHex(encoder.digest(password.toByteArray())).toLowerCase()
        encoder.reset()
        val auth = binToHex(encoder.digest((time + passwordEncoded).toByteArray())).toLowerCase()
        return ampacheApi
                .authenticate(limit = user, auth = auth, time = time)
                .doOnNext {
                    result ->
                    authSession = result.auth
                }
    }

    fun getSongs(from: String): Observable<SongList> {
        return ampacheApi.getSongs(auth = authSession, update = from)
    }

    fun getArtists(from: String): Observable<ArtistList> {
        return ampacheApi.getArtists(auth = authSession, update = from)
    }

    fun getAlbums(from: String): Observable<AlbumList> {
        return ampacheApi.getAlbums(auth = authSession, update = from)
    }

    fun getTags(from: String): Observable<TagList> {
        return ampacheApi.getTags(auth = authSession, update = from)
    }

    fun getPlaylists(from : String): Observable<PlaylistList> {
        return ampacheApi.getPlaylists(auth = authSession, update = from)
    }

    fun getSong(uid: Long): Observable<SongList> {
        return ampacheApi.getSong(auth = authSession, uid = uid)
    }

    private fun binToHex(data: ByteArray): String {
        return String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
    }
}