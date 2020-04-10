package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.model.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Ampache
 */
interface AmpacheApi {
    @GET("server/xml.server.php")
    suspend fun authenticate(
            @Query("action") action: String = "handshake",
            @Query("timestamp") time: String,
            @Query("version") version: String = "380001",
            @Query("auth") auth: String,
            @Query("user") user: String)
            : AmpacheAuthentication

    @GET("server/xml.server.php")
    suspend fun ping(
            @Query("action") action: String = "ping",
            @Query("auth") auth: String)
            : AmpachePing

    @GET("server/xml.server.php")
    suspend fun getSongs(
            @Query("action") action: String = "songs",
            @Query("add") add: String = "1970-01-01",
            @Query("auth") auth: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int)
            : AmpacheSongList

    @GET("server/xml.server.php")
    suspend fun getArtists(
            @Query("action") action: String = "artists",
            @Query("add") add: String = "1970-01-01",
            @Query("auth") auth: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int)
            : AmpacheArtistList

    @GET("server/xml.server.php")
    suspend fun getAlbums(
            @Query("action") action: String = "albums",
            @Query("add") add: String = "1970-01-01",
            @Query("auth") auth: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int)
            : AmpacheAlbumList

    @GET("server/xml.server.php")
    suspend fun getTags(
            @Query("action") action: String = "tags",
            @Query("add") add: String = "1970-01-01",
            @Query("auth") auth: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int)
            : AmpacheTagList

    @GET("server/xml.server.php")
    suspend fun getPlaylists(
            @Query("action") action: String = "playlists",
            @Query("add") add: String = "1970-01-01",
            @Query("auth") auth: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int)
            : AmpachePlayListList
}