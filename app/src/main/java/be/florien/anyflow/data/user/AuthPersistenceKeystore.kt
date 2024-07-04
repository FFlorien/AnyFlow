package be.florien.anyflow.data.user

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import be.florien.anyflow.data.TimeOperations
import be.florien.anyflow.extension.applyPutLong
import be.florien.anyflow.logging.eLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.InvalidKeyException
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableEntryException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.GCMParameterSpec


/**
 * Manager for all things authentication related
 */
class AuthPersistenceKeystore(
        private var preference: SharedPreferences,
        context: Context) : AuthPersistence() {
    companion object {
        private const val KEYSTORE_NAME = "AndroidKeyStore"
        private const val SERVER_FILENAME = "server"
        private const val USER_FILENAME = "user"
        private const val PASSWORD_FILENAME = "password"
        private const val AUTH_FILENAME = "auth"
        private const val API_TOKEN_FILENAME = "apiToken"

        private const val SERVER_ALIAS = "ServerUrl"
        private const val USER_ALIAS = USER_FILENAME
        private const val AUTH_ALIAS = "authData"
        private const val API_TOKEN_ALIAS = API_TOKEN_FILENAME
        private const val AES_CIPHER = "AES/GCM/NoPadding"
        private const val FIXED_IV = "turlutututut"
    }

    /**
     * Fields
     */

    private val dataDirectoryPath: String = context.filesDir.absolutePath + File.separator
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_NAME).apply {
        load(null)
    }
    override var serverUrl = EncryptedSecret(SERVER_ALIAS, SERVER_FILENAME)
    override var authToken = EncryptedSecret(AUTH_ALIAS, AUTH_FILENAME)
    override var user = EncryptedSecret(USER_ALIAS, USER_FILENAME)
    override var password = EncryptedSecret(USER_ALIAS, PASSWORD_FILENAME)
override var apiToken = EncryptedSecret(API_TOKEN_ALIAS, API_TOKEN_FILENAME)

    inner class EncryptedSecret(private val alias: String, private val filename: String) : ExpirationSecret {
        override var secret: String = ""
            get() {
                var secretValue = field
                if (secretValue.isBlank()) {
                    secretValue = if (keyStore.containsAlias(alias)) {
                        decryptSecret()
                    } else {
                        ""
                    }
                }
                field = secretValue
                return secretValue
            }
            private set(value) {
                encryptSecret(value, TimeOperations.getDateFromMillis(expiration).timeInMillis)
                field = value
            }
        override var expiration: Long = 0L
            get() = preference.getLong(alias, field)
            private set(value) {
                preference.applyPutLong(alias, value)
            }

        override fun setSecretData(secret: String, expiration: Long) {
            this.expiration = expiration
            this.secret = secret
        }

        override fun hasSecret(): Boolean = keyStore.containsAlias(alias)
        override fun isDataValid(): Boolean = TimeOperations.getDateFromMillis(expiration).after(TimeOperations.getCurrentDate())

        private fun getRsaKey(alias: String): Key? = keyStore.getKey(alias, null)

        @Throws(KeyStoreException::class, UnrecoverableEntryException::class, NoSuchAlgorithmException::class, NoSuchProviderException::class, NoSuchPaddingException::class, InvalidKeyException::class, IOException::class)
        private fun encryptSecret(secret: String, expiration: Long) {
            ensureRsaKey(expiration)
            val privateKeyEntry = getRsaKey(alias)
            val cipher = Cipher.getInstance(AES_CIPHER)
            cipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry, GCMParameterSpec(128, FIXED_IV.toByteArray()))
            val encryptedSecret = cipher.doFinal(secret.toByteArray())

            val outputStream = FileOutputStream(dataDirectoryPath + filename)
            outputStream.write(encryptedSecret)
            outputStream.flush()
            outputStream.close()
        }

        private fun decryptSecret(): String {
            try {
                val privateKeyEntry = getRsaKey(alias)
                val cipher = Cipher.getInstance(AES_CIPHER)
                cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry, GCMParameterSpec(128, FIXED_IV.toByteArray()))

                val inputStream = FileInputStream(dataDirectoryPath + filename)
                val encryptedSecret = ByteArray(1024)
                val bytesRead = inputStream.read(encryptedSecret)
                val decryptedSecret = cipher.doFinal(encryptedSecret, 0, bytesRead)
                return String(decryptedSecret)
            } catch (exception: Exception) {
                this@AuthPersistenceKeystore.eLog(exception, "Error while trying to retrieve a secured data")
                val file = File(dataDirectoryPath + filename)
                if (file.exists()) {
                    file.delete()
                }
                return ""
            }
        }

        private fun ensureRsaKey(expiration: Long) {
            if (!keyStore.containsAlias(alias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_NAME)
                keyGenerator.init(getSpecFromKeyGenParameter())
                keyGenerator.generateKey()

                preference.applyPutLong(alias, expiration)
            }
        }

        private fun getSpecFromKeyGenParameter() =
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(false)
                        .build()
    }
}