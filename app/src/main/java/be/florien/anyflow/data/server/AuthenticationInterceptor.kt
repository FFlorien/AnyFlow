package be.florien.anyflow.data.server

import be.florien.anyflow.data.user.AuthPersistence
import okhttp3.Interceptor
import okhttp3.Response

class AuthenticationInterceptor(private val authPersistence: AuthPersistence) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val nonAuthenticatedRequest = chain.request()
        val authToken = authPersistence.authToken
        if (!authToken.hasSecret() || !authToken.isDataValid() ) {
            return chain.proceed(nonAuthenticatedRequest)
        }

        val nonAuthenticatedUrl = nonAuthenticatedRequest.url
        val authenticatedUrl = nonAuthenticatedUrl
            .newBuilder()
            .addQueryParameter("auth", authToken.secret)
            .build()
        val authenticatedRequest = nonAuthenticatedRequest
            .newBuilder()
            .url(authenticatedUrl)
            .build()
        return chain.proceed(authenticatedRequest)
    }


}