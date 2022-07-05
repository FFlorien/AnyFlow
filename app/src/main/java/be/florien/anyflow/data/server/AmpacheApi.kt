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
    suspend fun getNewSongs(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "songs"
    ): AmpacheSongResponse

    @GET("server/json.server.php")
    suspend fun getNewGenres(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "genres"
    ): AmpacheGenreResponse

    @GET("server/json.server.php")
    suspend fun getNewArtists(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "artists"
    ): AmpacheArtistResponse

    @GET("server/json.server.php")
    suspend fun getNewAlbums(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "albums"
    ): AmpacheAlbumResponse

    @GET("server/json.server.php")
    suspend fun getNewPlaylists(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("hide_search") hideSearch: Int = 1,
        @Query("action") action: String = "playlists"
    ): AmpachePlaylistResponse

    @GET("server/json.server.php")
    suspend fun getAddedSongs(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("add") update: String = "1970-01-01",
        @Query("action") action: String = "songs"
    ): AmpacheSongResponse

    @GET("server/json.server.php")
    suspend fun getAddedGenres(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("add") update: String = "1970-01-01",
        @Query("action") action: String = "genres"
    ): AmpacheGenreResponse

    @GET("server/json.server.php")
    suspend fun getAddedArtists(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("add") update: String = "1970-01-01",
        @Query("action") action: String = "artists"
    ): AmpacheArtistResponse

    @GET("server/json.server.php")
    suspend fun getAddedAlbums(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("add") update: String = "1970-01-01",
        @Query("action") action: String = "albums"
    ): AmpacheAlbumResponse

    @GET("server/json.server.php")
    suspend fun getAddedPlaylists(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("add") update: String = "1970-01-01",
        @Query("hide_search") hideSearch: Int = 1,
        @Query("action") action: String = "playlists"
    ): AmpachePlaylistResponse

    @GET("server/json.server.php")
    suspend fun getUpdatedSongs(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("update") update: String = "1970-01-01",
        @Query("action") action: String = "songs"
    ): AmpacheSongResponse

    @GET("server/json.server.php")
    suspend fun getUpdatedGenres(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("update") update: String = "1970-01-01",
        @Query("action") action: String = "genres"
    ): AmpacheGenreResponse

    @GET("server/json.server.php")
    suspend fun getUpdatedArtists(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("update") update: String = "1970-01-01",
        @Query("action") action: String = "artists"
    ): AmpacheArtistResponse

    @GET("server/json.server.php")
    suspend fun getUpdatedAlbums(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("update") update: String = "1970-01-01",
        @Query("action") action: String = "albums"
    ): AmpacheAlbumResponse

    @GET("server/json.server.php")
    suspend fun getUpdatedPlaylists(
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("update") update: String = "1970-01-01",
        @Query("hide_search") hideSearch: Int = 1,
        @Query("action") action: String = "playlists"
    ): AmpachePlaylistResponse

    @GET("server/json.server.php")
    suspend fun getPlaylistSongs(
        @Query("action") action: String = "playlist_songs",
        @Query("filter") filter: String,
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): AmpacheSongIdResponse

    @GET("server/json.server.php")
    suspend fun getDeletedSongs(
        @Query("action") action: String = "deleted_songs",
        @Query("auth") auth: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): AmpacheDeletedSongIdResponse

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
}