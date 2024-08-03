package be.florien.anyflow.data.server.datasource.data

import be.florien.anyflow.data.server.model.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Ampache
 */
interface AmpacheDataApi {

    @GET("server/json.server.php")
    suspend fun getNewSongs(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "songs"
    ): AmpacheSongResponse

    @GET("server/json.server.php")
    suspend fun getNewGenres(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "genres"
    ): AmpacheGenreResponse

    @GET("server/json.server.php")
    suspend fun getNewArtists(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "artists"
    ): AmpacheArtistResponse

    @GET("server/json.server.php")
    suspend fun getNewAlbums(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "albums"
    ): AmpacheAlbumResponse

    @GET("server/json.server.php")
    suspend fun getPlaylists(
        @Query("action") action: String = "playlists",
        @Query("hide_search") hideSearch: Int = 1
    ): AmpachePlaylistResponse

    @GET("server/json.server.php")
    suspend fun getPlaylistsSongs(
        @Query("action") action: String = "index",
        @Query("type") type: String = "playlist",
        @Query("include") include: Int = 1,
        @Query("hide_search") hideSearch: Int = 1
    ): AmpachePlaylistsWithSongsResponse

    @GET("server/json.server.php")
    suspend fun getAddedSongs(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("add") update: String = "1970-01-01",
        @Query("action") action: String = "songs"
    ): AmpacheSongResponse

    @GET("server/json.server.php")
    suspend fun getAddedGenres(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("add") update: String = "1970-01-01",
        @Query("action") action: String = "genres"
    ): AmpacheGenreResponse

    @GET("server/json.server.php")
    suspend fun getAddedArtists(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("add") update: String = "1970-01-01",
        @Query("action") action: String = "artists"
    ): AmpacheArtistResponse

    @GET("server/json.server.php")
    suspend fun getAddedAlbums(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("add") update: String = "1970-01-01",
        @Query("action") action: String = "albums"
    ): AmpacheAlbumResponse

    @GET("server/json.server.php")
    suspend fun getUpdatedSongs(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("update") update: String = "1970-01-01",
        @Query("action") action: String = "songs"
    ): AmpacheSongResponse

    @GET("server/json.server.php")
    suspend fun getUpdatedGenres(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("update") update: String = "1970-01-01",
        @Query("action") action: String = "genres"
    ): AmpacheGenreResponse

    @GET("server/json.server.php")
    suspend fun getUpdatedArtists(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("update") update: String = "1970-01-01",
        @Query("action") action: String = "artists"
    ): AmpacheArtistResponse

    @GET("server/json.server.php")
    suspend fun getUpdatedAlbums(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("update") update: String = "1970-01-01",
        @Query("action") action: String = "albums"
    ): AmpacheAlbumResponse

    @GET("server/json.server.php")
    suspend fun getDeletedSongs(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "deleted_songs"
    ): AmpacheDeletedSongIdResponse

    @GET("server/json.server.php")
    suspend fun streamError(
        @Query("action") action: String = "stream",
        @Query("type") type: String = "song",
        @Query("id") songId: Long
    ): AmpacheErrorObject
}