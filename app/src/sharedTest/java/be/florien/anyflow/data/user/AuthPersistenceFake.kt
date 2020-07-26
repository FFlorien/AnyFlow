package be.florien.anyflow.data.user

import be.florien.anyflow.data.TimeOperations

class AuthPersistenceFake : AuthPersistence() {

    override val serverUrl = InMemorySecret()
    override val authToken = InMemorySecret()
    override val user = InMemorySecret()
    override val password = InMemorySecret()

    inner class InMemorySecret() : ExpirationSecret {
        override var secret: String = ""
            get() = if (TimeOperations.getCurrentDate().after(TimeOperations.getDateFromMillis(expiration))) "" else field
        override var expiration: Long = 0L

        override fun setSecretData(secret: String, expiration: Long) {
            this.expiration = expiration
            this.secret = secret
        }

        override fun hasSecret(): Boolean = secret.isNotEmpty()
        override fun isDataValid(): Boolean = TimeOperations.getDateFromMillis(expiration).after(TimeOperations.getCurrentDate())
    }

}