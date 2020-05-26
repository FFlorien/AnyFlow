package be.florien.anyflow.data.user

class AuthPersistenceFake : AuthPersistence() {

    override val serverUrl = InMemorySecret()
    override val authToken = InMemorySecret()
    override val user = InMemorySecret()
    override val password = InMemorySecret()

    inner class InMemorySecret() : ExpirationSecret {
        override var secret: String = ""
        override var expiration: Long = 0L

        override fun setSecretData(secret: String, expiration: Long) {
            this.expiration = expiration
            this.secret = secret
        }

        override fun hasSecret(): Boolean = secret.isNotEmpty()
    }

}