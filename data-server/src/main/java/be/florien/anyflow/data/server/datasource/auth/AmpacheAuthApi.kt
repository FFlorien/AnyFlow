package be.florien.anyflow.data.server.datasource.auth

import be.florien.anyflow.data.server.model.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Ampache
 */
interface AmpacheAuthApi {
    @GET("server/json.server.php")
    suspend fun authenticateUserPassword(
        @Query("action") action: String = "handshake",
        @Query("timestamp") time: String,
        @Query("auth") auth: String,
        @Query("user") user: String
    ): AmpacheAuthentication

    @GET("server/json.server.php")
    suspend fun authenticateApiToken(
        @Query("action") action: String = "handshake",
        @Query("auth") auth: String
    ): AmpacheAuthentication

    @GET("server/json.server.php")
    suspend fun authenticatedPing(
        @Query("action") action: String = "ping",
        @Query("auth") auth: String
    ): AmpacheAuthenticatedStatus

    @GET("server/json.server.php")
    suspend fun ping(
        @Query("action") action: String = "ping"
    ): AmpacheStatus
}