package be.florien.anyflow.data.user

import be.florien.anyflow.data.TimeOperations
import java.util.*

abstract class AuthPersistence {

    abstract val serverUrl: ExpirationSecret
    abstract val authToken: ExpirationSecret
    abstract val user: ExpirationSecret
    abstract val password: ExpirationSecret


    fun hasConnectionInfo() = serverUrl.hasSecret() && (authToken.hasSecret() && authToken.isDataValid()) || (user.hasSecret() && user.isDataValid())

    fun saveServerInfo(serverUrl: String) {
        val oneYearDate = TimeOperations.getCurrentDatePlus(Calendar.YEAR, 1)
        this.serverUrl.setSecretData(serverUrl, oneYearDate.timeInMillis)
    }

    fun saveConnectionInfo(user: String, password: String, authToken: String, expirationDate: Long) {
        val oneYearDate = TimeOperations.getCurrentDatePlus(Calendar.YEAR, 1)
        this.user.setSecretData(user, oneYearDate.timeInMillis)
        this.password.setSecretData(password, oneYearDate.timeInMillis)
        this.authToken.setSecretData(authToken, expirationDate)
    }

    fun setNewAuthExpiration(expirationDate: Long) {
        if (authToken.hasSecret()) {
            authToken.setSecretData(authToken.secret, expirationDate)
        }
    }

    fun revokeAuthToken() {
        this.authToken.setSecretData("", TimeOperations.getCurrentDatePlus(Calendar.YEAR, -1).timeInMillis)
    }

    interface ExpirationSecret {
        val secret: String
        val expiration: Long

        fun setSecretData(secret: String, expiration: Long)
        fun hasSecret(): Boolean

        fun isDataValid(): Boolean
    }
}