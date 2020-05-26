package be.florien.anyflow.data.user

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

abstract class AuthPersistence {
    abstract val serverUrl: ExpirationSecret
    abstract val authToken: ExpirationSecret
    abstract val user: ExpirationSecret
    abstract val password: ExpirationSecret

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", Locale.US)

    fun hasConnectionInfo() = serverUrl.hasSecret() && (authToken.hasSecret() && authToken.isDataValid()) || (user.hasSecret() && user.isDataValid())

    fun saveServerInfo(serverUrl: String) {
        val oneYearDate = Calendar.getInstance()
        oneYearDate.add(Calendar.YEAR, 1)
        this.serverUrl.setSecretData(serverUrl, oneYearDate.timeInMillis)
    }

    fun saveConnectionInfo(user: String, password: String, authToken: String, expirationDate: String) {
        val oneYearDate = Calendar.getInstance()
        oneYearDate.add(Calendar.YEAR, 1)
        this.user.setSecretData(user, oneYearDate.timeInMillis)
        this.password.setSecretData(password, oneYearDate.timeInMillis)
        this.authToken.setSecretData(authToken, (dateFormatter.parse(expirationDate)?.time ?: 0L))
    }

    fun setNewAuthExpiration(expirationDate: String) {
        if (authToken.hasSecret()) {
            val newExpiration = try {
                dateFormatter.parse(expirationDate)?.time ?: 0L
            } catch (exception: ParseException) {
                authToken.expiration
            }
            authToken.setSecretData(authToken.secret, newExpiration)
        }
    }

    fun revokeAuthToken() {
        this.authToken.setSecretData("", Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis)
    }

    interface ExpirationSecret {
        val secret: String
        val expiration: Long

        fun setSecretData(secret: String, expiration: Long)
        fun hasSecret(): Boolean

        fun isDataValid() = Date(expiration).after(Date())
    }
}