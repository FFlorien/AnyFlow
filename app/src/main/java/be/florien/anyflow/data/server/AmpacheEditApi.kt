package be.florien.anyflow.data.server

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Ampache
 */
interface AmpacheEditApi {

    @GET("server/json.server.php")
    suspend fun createPlaylist(
        @Query("action") action: String = "playlist_create",
        @Query("name") name: String,
        @Query("type") type: String = "private"
    )

    @GET("server/json.server.php")
    suspend fun deletePlaylist(
        @Query("action") action: String = "playlist_delete",
        @Query("filter") id: String
    )

    @GET("server/json.server.php")
    suspend fun removeFromPlaylist(
        @Query("action") action: String = "playlist_remove_song",
        @Query("filter") filter: Long,
        @Query("song") song: Long
    )

    @GET("server/json.server.php")
    suspend fun editPlaylist(
        @Query("action") action: String = "playlist_edit",
        @Query("filter") playlistId: String,
        @Query("items") items: String,
        @Query("tracks") tracks: String,
    )
}