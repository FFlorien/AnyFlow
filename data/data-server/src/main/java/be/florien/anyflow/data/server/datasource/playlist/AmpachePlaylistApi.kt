package be.florien.anyflow.data.server.datasource.playlist

import be.florien.anyflow.data.server.model.AmpacheSuccessResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Ampache
 */
interface AmpachePlaylistApi {

    @GET("server/json.server.php")
    suspend fun createPlaylist( //todo response
        @Query("action") action: String = "playlist_create",
        @Query("name") name: String,
        @Query("type") type: String = "private"
    )

    @GET("server/json.server.php")
    suspend fun deletePlaylist(
        @Query("action") action: String = "playlist_delete",
        @Query("filter") id: String
    ): AmpacheSuccessResponse

    @GET("server/json.server.php")
    suspend fun removeFromPlaylist(
        @Query("action") action: String = "playlist_remove_song",
        @Query("filter") filter: Long,
        @Query("song") song: Long
    ): AmpacheSuccessResponse

    @GET("server/json.server.php")
    suspend fun addToPlaylist(
        @Query("action") action: String = "playlist_add",
        @Query("filter") playlistId: String,
        @Query("id") item: String,
        @Query("type") type: String,
    ): AmpacheSuccessResponse

    @GET("server/json.server.php")
    suspend fun editPlaylist(
        @Query("action") action: String = "playlist_edit",
        @Query("filter") playlistId: String,
        @Query("items") items: String,
        @Query("tracks") tracks: String,
    ): AmpacheSuccessResponse
}