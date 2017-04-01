package be.florien.ampacheplayer.model.retrofit

import be.florien.ampacheplayer.model.server.*
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
            @Query("user") limit: String)
            : Observable<Authentication>

    @GET("server/xml.server.php")
    fun getSongs(
            @Query("action") action: String = "songs",
            @Query("update") update: String = "1970-01-01",
            @Query("auth") auth: String)
            : Observable<SongList>

    @GET("server/xml.server.php")
    fun getArtists(
            @Query("action") action: String = "artists",
            @Query("auth") auth: String)
            : Observable<ArtistList>

    @GET("server/xml.server.php")
    fun getAlbums(
            @Query("action") action: String = "albums",
            @Query("auth") auth: String)
            : Observable<AlbumList>

    @GET("server/xml.server.php")
    fun getTags(
            @Query("action") action: String = "tags",
            @Query("auth") auth: String)
            : Observable<TagList>

    @GET("server/xml.server.php")
    fun getPlaylists(
            @Query("action") action: String = "playlists",
            @Query("auth") auth: String)
            : Observable<PlaylistList>

    @GET("server/xml.server.php")
    fun getSong(
            @Query("action") action: String = "song",
            @Query("filter") uid: Long,
            @Query("auth") auth: String)
            : Observable<SongList>
}