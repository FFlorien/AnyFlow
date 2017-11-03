package be.florien.ampacheplayer.manager

import android.content.Context
import android.content.SharedPreferences
import android.security.KeyPairGeneratorSpec
import be.florien.ampacheplayer.extension.applyPutLong
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.NoSuchPaddingException
import javax.inject.Inject
import javax.security.auth.x500.X500Principal


/**
 * Manager for all things authentication related
 */
class AuthManager
@Inject constructor(
        private var preference: SharedPreferences,
        var context: Context) {

    /**
     * Constants
     */
    companion object {

        private const val ALGORITHM_NAME = "RSA"
        private const val KEYSTORE_NAME = "AndroidKeyStore"
        private const val USER_FILENAME = "user"
        private const val PASSWORD_FILENAME = "password"
        private const val AUTH_FILENAME = "auth"

        private const val USER_ALIAS = USER_FILENAME
        private const val AUTH_ALIAS = "authData"
        private const val RSA_CIPHER = "RSA/ECB/PKCS1Padding"
        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", Locale.US)
    }

    /**
     * Fields
     */

    private val dataDirectoryPath: String = context.filesDir.absolutePath + File.separator
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_NAME).apply {
        load(null)
    }
    var authToken: String = ""
        get() {
            if (field.isBlank()) {
                field = getEncryptedData(field, AUTH_ALIAS, AUTH_FILENAME)
            }
            return field
        }
    var user: String = ""
        get() {
            if (field.isBlank()) {
                field = getEncryptedData(field, USER_ALIAS, USER_FILENAME)
            }
            return field
        }
    var password: String = ""
        get() {
            if (field.isBlank()) {
                field = getEncryptedData(field, USER_ALIAS, PASSWORD_FILENAME)
            }
            return field
        }

    private fun getEncryptedData(propertyField: String, alias: String, filename: String): String {
        return if (propertyField.trim() != "") {
            propertyField
        } else {
            if (keyStore.containsAlias(alias) && isDataValid(alias)) {
                decryptSecret(alias, filename)
            } else {
                ""
            }
        }
    }

    /**
     * Public methods
     */
    fun hasConnectionInfo() = (keyStore.containsAlias(AUTH_ALIAS) && isDataValid(AUTH_ALIAS)) || (keyStore.containsAlias(USER_ALIAS) && isDataValid(USER_ALIAS))


    fun authenticate(user: String, password: String, authToken: String, expirationDate: String) {
        this.authToken = authToken
        val oneYearDate = Calendar.getInstance()
        oneYearDate.add(Calendar.YEAR, 1)
        encryptSecret(authToken, AUTH_ALIAS, AUTH_FILENAME, DATE_FORMATTER.parse(expirationDate))
        encryptSecret(user, USER_ALIAS, USER_FILENAME, oneYearDate.time)
        encryptSecret(password, USER_ALIAS, PASSWORD_FILENAME, oneYearDate.time)
    }

    fun extendsSession(expirationDate: String) {
        if (keyStore.containsAlias(AUTH_ALIAS)) {
            val newExpiration = try {
                DATE_FORMATTER.parse(expirationDate).time
            } catch (exception: ParseException) {
                preference.getLong(AUTH_ALIAS, 0)
            }
            preference.applyPutLong(AUTH_ALIAS, newExpiration)
        }
    }

    /**
     * Private methods
     */

    private fun getRsaKey(alias: String): KeyStore.PrivateKeyEntry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry


    private fun isDataValid(alias: String) = Date(preference.getLong(alias, 0L)).after(Date())

    @Throws(KeyStoreException::class, UnrecoverableEntryException::class, NoSuchAlgorithmException::class, NoSuchProviderException::class, NoSuchPaddingException::class, InvalidKeyException::class, IOException::class)
    private fun encryptSecret(secret: String, alias: String, filename: String, expiration: Date) {
        renewRsaKey(alias, expiration)
        val privateKeyEntry = getRsaKey(alias)
        val cipher = Cipher.getInstance(RSA_CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.certificate.publicKey)

        val outputStream = FileOutputStream(dataDirectoryPath + filename)
        val cipherOutputStream = CipherOutputStream(outputStream, cipher)
        cipherOutputStream.write(secret.toByteArray())
        cipherOutputStream.flush()
        cipherOutputStream.close()
    }

    private fun decryptSecret(alias: String, filename: String): String {
        try {
            val privateKeyEntry = getRsaKey(alias)
            val cipher = Cipher.getInstance(RSA_CIPHER)
            cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)

            val inputStream = FileInputStream(dataDirectoryPath + filename)
            val cipherInputStream = CipherInputStream(inputStream, cipher)
            val goodBytes = cipherInputStream.readBytes()
            cipherInputStream.close()
            return String(goodBytes, Charsets.UTF_8)
        } catch (exception: Exception) {
            val file = File(dataDirectoryPath + filename)
            if (file.exists()) {
                file.delete()
            }
            return ""
        }
    }

    @Suppress("DEPRECATION")
    private fun renewRsaKey(alias: String, expiration: Date) {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
        val nowDate = Date()

        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_NAME, KEYSTORE_NAME)

        val keySpec = KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setStartDate(nowDate)
                .setEndDate(expiration)
                .setSubject(X500Principal("C=BE, CN=ampachePlayer"))
                .setSerialNumber(BigInteger.valueOf(1337))
                .build()
        keyPairGenerator.initialize(keySpec)
        keyPairGenerator.generateKeyPair()
        preference.applyPutLong(alias, expiration.time)
    }

}