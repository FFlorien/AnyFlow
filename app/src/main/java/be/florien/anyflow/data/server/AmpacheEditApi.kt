package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.model.*
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
    suspend fun addToPlaylist(
        @Query("action") action: String = "playlist_add_song",
        @Query("filter") filter: Long,
        @Query("song") songId: Long,
        @Query("check") check: Int = 1
    )

    @GET("server/json.server.php")
    suspend fun removeFromPlaylist(
        @Query("action") action: String = "playlist_remove_song",
        @Query("filter") filter: Long,
        @Query("song") song: Long
    )
}