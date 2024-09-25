package be.florien.anyflow.data.server.datasource.podcast

import be.florien.anyflow.data.server.model.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Ampache
 */
interface AmpachePodcastApi {
    @GET("server/json.server.php")
    suspend fun getPodcasts(
        @Query("action") action: String = "podcasts",
    ): AmpachePodcastsResponse

    @GET("server/json.server.php")
    suspend fun getPodcastsWithEpisode(
        @Query("action") action: String = "podcasts",
        @Query("include") auth: String = "episodes"
    ): AmpachePodcastsResponse

    @GET("server/json.server.php")
    suspend fun updatePodcast(
        @Query("action") action: String = "update_podcast",
        @Query("filter") id: String
    )
}