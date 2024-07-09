package be.florien.anyflow.feature.auth.domain.net

import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.data.server.model.AmpacheErrorObject
import be.florien.anyflow.feature.auth.domain.persistence.AuthPersistence
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.utils.TimeOperations
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

@ServerScope
class AuthenticationInterceptor @Inject constructor(
    private val authPersistence: AuthPersistence,
    private val authRepository: Lazy<AuthRepository>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val nonAuthenticatedRequest = chain.request()
        val authToken = authPersistence.authToken
        val hasAuthToken = authToken.hasSecret()

        if (!hasAuthToken || !authToken.isDataValid()) {
            return if (hasAuthToken && isCurrentTokenExtended()) {
                chain.proceedWithValidToken(authToken)
            } else {
                synchronized(this) {
                    val maybeFreshAuthToken = authPersistence.authToken
                    if (authToken.hasSecret() && authToken.isDataValid()) {
                        chain.proceedWithValidToken(maybeFreshAuthToken)
                    } else {
                        try {
                            chain.proceedWithReconnectTokens()
                        } catch (exception: Exception) {
                            chain.proceed(nonAuthenticatedRequest)
                        }
                    }
                }
            }
        }

        return chain.proceedWithValidToken(authToken)
    }

    private fun isCurrentTokenExtended(): Boolean {
        return try {
            val status = runBlocking { authRepository.get().authenticatedPing() }
            TimeOperations.getDateFromAmpacheComplete(status.session_expire) > TimeOperations.getCurrentDate()
        } catch (throwable: Throwable) {
            false
        }
    }

    private fun Interceptor.Chain.proceedWithReconnectTokens(): Response {
        val newToken = runBlocking {
            authRepository.get().reconnect {
                authPersistence.authToken
            }
        }
        return proceedWithValidToken(newToken)
    }

    private fun Interceptor.Chain.proceedWithValidToken(authToken: AuthPersistence.ExpirationSecret): Response {
        val authenticatedRequest = request()
            .newBuilder()
            .header("Authorization", "Bearer ${authToken.secret}")
            .build()
        val response = proceed(authenticatedRequest)
        checkError(response)
        return response
    }

    private fun checkError(response: Response) {
        try {
            val peekBody = response.peekBody(1000).byteStream()
            val error = ObjectMapper().readValue(peekBody, AmpacheErrorObject::class.java)
            if (error.error.errorCode == 4701) {
                authPersistence.revokeAuthToken()
            }
        } catch (exception: Exception) {
            // ignore
        }
    }
}