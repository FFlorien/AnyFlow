package be.florien.ampacheplayer.manager

import android.content.Context
import android.content.SharedPreferences
import android.security.KeyPairGeneratorSpec
import be.florien.ampacheplayer.extension.applyPutLong
import timber.log.Timber
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
import kotlin.reflect.KProperty

private const val ALGORITHM_NAME = "RSA"
private const val KEYSTORE_NAME = "AndroidKeyStore"
private const val USER_FILENAME = "user"
private const val PASSWORD_FILENAME = "password"
private const val AUTH_FILENAME = "auth"

private const val USER_ALIAS = USER_FILENAME
private const val AUTH_ALIAS = "authData"
private const val RSA_CIPHER = "RSA/ECB/PKCS1Padding"

/**
 * Manager for all things authentication related
 */
class AuthManager
@Inject constructor(
        private var preference: SharedPreferences,
        private var context: Context) {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", Locale.US)

    /**
     * Fields
     */

    private val dataDirectoryPath: String = context.filesDir.absolutePath + File.separator
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_NAME).apply {
        load(null)
    }
    var authToken: Pair<String, Long> by Encrypted(AUTH_ALIAS, AUTH_FILENAME)
    var user: Pair<String, Long> by Encrypted(USER_ALIAS, USER_FILENAME)
    var password: Pair<String, Long> by Encrypted(USER_ALIAS, PASSWORD_FILENAME)

    /**
     * Public methods
     */
    fun hasConnectionInfo() = (keyStore.containsAlias(AUTH_ALIAS) && isDataValid(AUTH_ALIAS)) || (keyStore.containsAlias(USER_ALIAS) && isDataValid(USER_ALIAS))

    fun authenticate(user: String, password: String, authToken: String, expirationDate: String) {
        val oneYearDate = Calendar.getInstance()
        oneYearDate.add(Calendar.YEAR, 1)
        this.user = user to oneYearDate.timeInMillis
        this.password = password to oneYearDate.timeInMillis
        this.authToken = authToken to dateFormatter.parse(expirationDate).time
    }

    fun setNewAuthExpiration(expirationDate: String) { // I don't remember how the hell this is supposed to work
        if (keyStore.containsAlias(AUTH_ALIAS)) {
            val newExpiration = try {
                dateFormatter.parse(expirationDate).time
            } catch (exception: ParseException) {
                preference.getLong(AUTH_ALIAS, 0)
            }
            preference.applyPutLong(AUTH_ALIAS, newExpiration)
        }
    }

    /**
     * Private methods
     */

    private fun isDataValid(alias: String) = Date(preference.getLong(alias, 0L)).after(Date())

    inner class Encrypted(private val alias: String, private val filename: String) {
        var value: Pair<String, Long> = "" to 0L

        operator fun getValue(thisRef: Any?, property: KProperty<*>): Pair<String, Long> {
            var token = value.first
            val expiration = preference.getLong(alias, value.second)
            if (token.isBlank()) {
                token = if (keyStore.containsAlias(alias) && isDataValid(alias)) {
                    decryptSecret(alias, filename)
                } else {
                    ""
                }
            }
            return token to expiration
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Pair<String, Long>) {
            encryptSecret(value.first, alias, filename, Date(value.second))
            preference.applyPutLong(alias, value.second)
            this.value = value
        }

        private fun getRsaKey(alias: String): KeyStore.PrivateKeyEntry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry

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
                Timber.e(exception, "Error while trying to retrieve a secured data")
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
}