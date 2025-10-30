package be.florien.anyflow.feature.auth.domain.repository

import be.florien.anyflow.data.server.datasource.auth.AmpacheAuthApi
import be.florien.anyflow.common.logging.eLog
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


@Singleton
class ServerValidator @Inject constructor(
    @param:Named("nonAuthenticated")
    var okHttpClient: OkHttpClient
) {

    suspend fun isServerValid(serverUrl: String): ServerStatus {
        val url = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        try {
            val retrofit = Retrofit
                .Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
            val authApi = retrofit.create(AmpacheAuthApi::class.java)

            val ping = authApi.ping()
            return if (ping.error.errorCode == 0) {
                ServerStatus.Success(url)
            } else {
                ServerStatus.ErrorConnectivity
            }
        } catch (exception: IllegalArgumentException) {
            val message = exception.message
            if (message == null) {
                eLog(message = "Empty message for IllegalArgumentException during server screen", t = exception)
                return ServerStatus.ErrorFormatUnknown
            }
            return when {
                message.contains("must end in /") -> ServerStatus.ErrorFormatEndSlash
                message.contains("Expected URL scheme 'http' or 'https'") -> ServerStatus.ErrorFormatHttp
                else -> {
                    eLog(message = "Unknown error for server definition", t = exception)
                    ServerStatus.ErrorFormatUnknown
                }
            }
        }  catch (_: UnknownHostException) {
            return ServerStatus.ErrorConnectivity
        }  catch (_: HttpException) {
            return ServerStatus.ErrorConnectivity
        }
    }

    sealed interface ServerStatus {
        class Success(val url: String): ServerStatus
        object ErrorFormatEndSlash: ServerStatus
        object ErrorFormatHttp: ServerStatus
        object ErrorFormatUnknown: ServerStatus
        object ErrorConnectivity: ServerStatus
    }
}