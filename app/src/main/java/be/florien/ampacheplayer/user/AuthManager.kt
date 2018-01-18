package be.florien.ampacheplayer.user

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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
import javax.crypto.NoSuchPaddingException
import javax.inject.Inject
import javax.inject.Singleton
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
@Singleton
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

    fun saveConnectionInfo(user: String, password: String, authToken: String, expirationDate: String) {
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
            val encryptedSecret = cipher.doFinal(secret.toByteArray())

            val outputStream = FileOutputStream(dataDirectoryPath + filename)
            outputStream.write(encryptedSecret)
            outputStream.flush()
            outputStream.close()
        }

        private fun decryptSecret(alias: String, filename: String): String {
            try {
                val privateKeyEntry = getRsaKey(alias)
                val cipher = Cipher.getInstance(RSA_CIPHER)
                cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)

                val inputStream = FileInputStream(dataDirectoryPath + filename)
                val encryptedSecret = ByteArray(1024)
                val bytesRead = inputStream.read(encryptedSecret)
                val decryptedSecret = cipher.doFinal(encryptedSecret, 0, bytesRead)
                return String(decryptedSecret)
            } catch (exception: Exception) {
                Timber.e(exception, "Error while trying to retrieve a secured data")
                val file = File(dataDirectoryPath + filename)
                if (file.exists()) {
                    //todo reactivate this when the mysterious error disappear -> file.delete()
                }
                return ""
            }
        }

        private fun renewRsaKey(alias: String, expiration: Date) {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
            val nowDate = Date()

            val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_NAME, KEYSTORE_NAME)

            val spec = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                getSpecFromKeyPairGenerator(alias, nowDate, expiration)
            } else {
                getSpecFromKeyGenParameter(alias, nowDate, expiration)
            }
            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()
            preference.applyPutLong(alias, expiration.time)
        }

        @Suppress("DEPRECATION")
        private fun getSpecFromKeyPairGenerator(alias: String, nowDate: Date, expiration: Date) =
                KeyPairGeneratorSpec.Builder(context)
                        .setAlias(alias)
                        .setStartDate(nowDate)
                        .setEndDate(expiration)
                        .setSubject(X500Principal("CN=$alias CA Certificate"))
                        .setSerialNumber(BigInteger.valueOf(1337))
                        .build()

        @TargetApi(Build.VERSION_CODES.M)
        private fun getSpecFromKeyGenParameter(alias: String, nowDate: Date, expiration: Date) =
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .setCertificateNotBefore(nowDate)
                        .setCertificateNotAfter(expiration)
                        .build()
    }
}