package be.florien.anyflow.data.user

import be.florien.anyflow.utils.TimeOperations
import java.util.*

abstract class AuthPersistence {

    abstract val serverUrl: ExpirationSecret
    abstract val authToken: ExpirationSecret
    abstract val user: ExpirationSecret
    abstract val password: ExpirationSecret
    abstract val apiToken: ExpirationSecret


    fun hasConnectionInfo() = serverUrl.hasSecret() && (hasAuthToken() || hasUser() || hasApiToken())

    private fun hasAuthToken() = (authToken.hasSecret() && authToken.isDataValid())

    private fun hasUser() = (user.hasSecret() && user.isDataValid())

    private fun hasApiToken() = (apiToken.hasSecret() && apiToken.isDataValid())

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

    fun saveConnectionInfo(apiToken: String, authToken: String, expirationDate: Long) {
        val oneYearDate = TimeOperations.getCurrentDatePlus(Calendar.YEAR, 1)
        this.apiToken.setSecretData(apiToken, oneYearDate.timeInMillis)
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