package be.florien.ampacheplayer.persistence.server

import be.florien.ampacheplayer.persistence.server.model.*
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Ampache
 */
interface AmpacheApi {
    @GET("server/xml.server.php")
    fun authenticate(
            @Query("action") action: String = "handshake",
            @Query("timestamp") time: String,
            @Query("version") version: String = "380001",
            @Query("auth") auth: String,
            @Query("user") user: String)
            : Observable<AmpacheAuthentication>

    @GET("server/xml.server.php")
    fun ping(
            @Query("action") action: String = "ping",
            @Query("auth") auth: String)
            : Observable<AmpachePing>

    @GET("server/xml.server.php")
    fun getSongs(
            @Query("action") action: String = "songs",
            @Query("update") update: String = "1970-01-01",
            @Query("auth") auth: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int)
            : Observable<AmpacheSongList>

    @GET("server/xml.server.php")
    fun getArtists(
            @Query("action") action: String = "artists",
            @Query("update") update: String = "1970-01-01",
            @Query("auth") auth: String)
            : Observable<AmpacheArtistList>

    @GET("server/xml.server.php")
    fun getAlbums(
            @Query("action") action: String = "albums",
            @Query("update") update: String = "1970-01-01",
            @Query("auth") auth: String)
            : Observable<AmpacheAlbumList>

    @GET("server/xml.server.php")
    fun getTags(
            @Query("action") action: String = "tags",
            @Query("update") update: String = "1970-01-01",
            @Query("auth") auth: String)
            : Observable<AmpacheTagList>

    @GET("server/xml.server.php")
    fun getPlaylists(
            @Query("action") action: String = "playlists",
            @Query("update") update: String = "1970-01-01",
            @Query("auth") auth: String)
            : Observable<AmpachePlayListList>

    @GET("server/xml.server.php")
    fun getSong(
            @Query("action") action: String = "song",
            @Query("filter") uid: Long,
            @Query("auth") auth: String)
            : Observable<AmpacheSongList>
}