package be.florien.anyflow.feature.auth.domain.repository

import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.data.server.datasource.auth.AmpacheAuthApi
import be.florien.anyflow.logging.eLog
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


@Singleton
class ServerValidator @Inject constructor(
    @Named("nonAuthenticated")
    var okHttpClient: OkHttpClient
) {

    suspend fun isServerValid(serverUrl: String): Boolean { //todo try catch and return enum with success and error cases
        val retrofit = Retrofit
            .Builder()
            .baseUrl(serverUrl)
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
        val authApi = retrofit.create(AmpacheAuthApi::class.java)

        return try {
            val ping = authApi.ping()
            ping.error.errorCode == 0
        } catch (exception: Exception) {
            eLog(exception)
            false
        }
    }
}