package be.florien.anyflow.data.server

import be.florien.anyflow.data.TimeOperations
import be.florien.anyflow.data.server.model.AmpacheAuthenticatedStatus
import be.florien.anyflow.data.server.model.AmpacheAuthentication
import be.florien.anyflow.data.server.model.AmpacheStatus
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ServerScope
import retrofit2.Retrofit
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Named

/**
 * Manager for the ampache API server-side
 */
@ServerScope
open class AmpacheAuthSource
@Inject constructor(@Named("nonAuthenticated") retrofit: Retrofit) {

    private var ampacheAuthApi: AmpacheAuthApi = retrofit.create(AmpacheAuthApi::class.java)

    /**
     * API calls : connection
     */
    suspend fun authenticate(user: String, password: String): AmpacheAuthentication {
        val time = (TimeOperations.getCurrentDate().timeInMillis / 1000).toString()
        val encoder = MessageDigest.getInstance("SHA-256")
        encoder.reset()
        val passwordEncoded =
            binToHex(encoder.digest(password.toByteArray())).lowercase(Locale.ROOT)
        encoder.reset()
        val auth =
            binToHex(encoder.digest((time + passwordEncoded).toByteArray())).lowercase(Locale.ROOT)
        try {
            return ampacheAuthApi.authenticateUserPassword(user = user, auth = auth, time = time)
        } catch (exception: Exception) {
            eLog(exception, "Unknown error while trying to login")
            throw exception
        }
    }

    suspend fun authenticate(apiToken: String): AmpacheAuthentication {
        try {
            return ampacheAuthApi.authenticateApiToken(auth = apiToken)
        } catch (exception: Exception) {
            eLog(exception, "Unknown error while trying to login")
            throw exception
        }
    }

    suspend fun authenticatedPing(authToken: String): AmpacheAuthenticatedStatus {
        try {
            return ampacheAuthApi.authenticatedPing(auth = authToken)
        } catch (exception: Exception) {
            eLog(exception)
            throw exception
        }
    }

    suspend fun ping(): AmpacheStatus = ampacheAuthApi.ping()

    private fun binToHex(data: ByteArray): String =
        String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
}
