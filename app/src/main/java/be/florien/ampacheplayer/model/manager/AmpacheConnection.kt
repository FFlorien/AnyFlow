package be.florien.ampacheplayer.model.manager

import be.florien.ampacheplayer.App
import be.florien.ampacheplayer.model.data.*
import be.florien.ampacheplayer.model.retrofit.AmpacheApi
import io.reactivex.Observable
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Manager for the ampache API server-side //todo update description when needed
 */
class AmpacheConnection {
    /**
     * Constants
     */
    private val LAST_UPDATE_KEY = "LAST_UPDATE"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Fields
     */
    var authToken: String = ""
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
        return ampacheApi.authenticate(limit = user, auth = auth, time = time)
    }

    fun getSongs(): Observable<SongList> {
        return ampacheApi.getSongs(auth = authToken)
    }

    fun getArtists(): Observable<ArtistList> {
        return ampacheApi.getArtists(auth = authToken)
    }

    fun getAlbums(): Observable<AlbumList> {
        return ampacheApi.getAlbums(auth = authToken)
    }

    fun getTags(): Observable<TagList> {
        return ampacheApi.getTags(auth = authToken)
    }

    fun getPlaylists(): Observable<PlaylistList> {
        return ampacheApi.getPlaylists(auth = authToken)
    }

    fun getSong(uid: Long): Observable<SongList> {
        return ampacheApi.getSong(auth = authToken, uid = uid)
    }

    private fun binToHex(data: ByteArray): String {
        return String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
    }
}