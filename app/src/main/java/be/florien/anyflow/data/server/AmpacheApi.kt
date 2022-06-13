package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.model.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Ampache
 */
interface AmpacheApi {
    @GET("server/json.server.php")
    suspend fun authenticate(
        @Query("action") action: String = "handshake",
        @Query("timestamp") time: String,
        @Query("version") version: String = "380001",
        @Query("auth") auth: String,
        @Query("user") user: String
    ): AmpacheAuthentication

    @GET("server/json.server.php")
    suspend fun ping(
        @Query("action") action: String = "ping",
        @Query("auth") auth: String
    ): AmpachePing

    @GET("server/json.server.php")
    suspend fun getSongsForFirstTime(
        @Query("action") action: String = "songs",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): AmpacheSongResponse

    @GET("server/json.server.php")
    suspend fun getGenresForFirstTime(
        @Query("action") action: String = "genres",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): AmpacheGenreResponse

    @GET("server/json.server.php")
    suspend fun getArtistsForFirstTime(
        @Query("action") action: String = "artists",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): AmpacheArtistResponse

    @GET("server/json.server.php")
    suspend fun getAlbumsForFirstTime(
        @Query("action") action: String = "albums",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): AmpacheAlbumResponse

    @GET("server/json.server.php")
    suspend fun getPlaylistsForFirstTime(
        @Query("action") action: String = "playlists",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("hide_search") hideSearch: Int = 1
    ): AmpachePlaylistResponse

    @GET("server/json.server.php")
    suspend fun getSongs(
        @Query("action") action: String = "songs",
        @Query("update") update: String = "1970-01-01",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): AmpacheSongResponse

    @GET("server/json.server.php")
    suspend fun getGenres(
        @Query("action") action: String = "genres",
        @Query("update") update: String = "1970-01-01",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): AmpacheGenreResponse

    @GET("server/json.server.php")
    suspend fun getArtists(
        @Query("action") action: String = "artists",
        @Query("update") update: String = "1970-01-01",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): AmpacheArtistResponse

    @GET("server/json.server.php")
    suspend fun getAlbums(
        @Query("action") action: String = "albums",
        @Query("update") update: String = "1970-01-01",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): AmpacheAlbumResponse

    @GET("server/json.server.php")
    suspend fun getPlaylists(
        @Query("action") action: String = "playlists",
        @Query("update") update: String = "1970-01-01",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("hide_search") hideSearch: Int = 1
    ): AmpachePlaylistResponse

    @GET("server/json.server.php")
    suspend fun getPlaylistSongs(
        @Query("action") action: String = "playlist_songs",
        @Query("filter") filter: String,
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    )
            : AmpacheSongIdResponse

    @GET("server/json.server.php")
    suspend fun createPlaylist(
        @Query("action") action: String = "playlist_create",
        @Query("auth") auth: String,
        @Query("name") name: String,
        @Query("type") type: String = "private"
    )

    @GET("server/json.server.php")
    suspend fun addToPlaylist(
        @Query("action") action: String = "playlist_add_song",
        @Query("filter") filter: Long,
        @Query("auth") auth: String,
        @Query("song") songId: Long,
        @Query("check") check: Int = 1
    )

    @GET("server/json.server.php")
    suspend fun getArtistWithId(
        @Query("action") action: String = "artist",
        @Query("filter") id: Long,
        @Query("auth") auth: String
    ): AmpacheArtist
}