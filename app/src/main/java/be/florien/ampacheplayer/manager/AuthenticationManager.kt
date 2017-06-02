package be.florien.ampacheplayer.manager

import android.content.Context
import android.content.SharedPreferences
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import be.florien.ampacheplayer.extension.applyPutLong
import io.reactivex.Observable
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
class AuthenticationManager
@Inject constructor(
        var preference: SharedPreferences,
        var connection: AmpacheConnection,
        var context: Context) {

    /**
     * Constants
     */
    private val ALGORITHM_NAME = "RSA"
    private val KEYSTORE_NAME = "AndroidKeyStore"
    private val USER_FILENAME = "user"
    private val PASSWORD_FILENAME = "password"
    private val AUTH_FILENAME = "auth"
    private val THIRTY_MINUTES = 1000 * 60 * 30

    private val USER_ALIAS = USER_FILENAME
    private val AUTH_ALIAS = "authData"
    private val RSA_CIPHER = "RSA/ECB/NoPadding"
    private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", Locale.US)

    /**
     * Fields
     */

    val dataDirectoryPath: String = context.filesDir.absolutePath + File.separator
    val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_NAME).apply {
        load(null)
    }

    /**
     * Public methods
     */
    fun isConnected() = (keyStore.containsAlias(AUTH_ALIAS) && isDataValid(AUTH_ALIAS)) || (keyStore.containsAlias(USER_ALIAS) && isDataValid(USER_ALIAS))


    fun authenticate(user: String, password: String): Observable<Boolean> {
        return connection
                .authenticate(user, password)
                .flatMap {
                    authentication ->
                    when (authentication.error.code) {
                        0 -> {
                            val oneYearDate = Calendar.getInstance()
                            oneYearDate.add(Calendar.YEAR, 1)
                            encryptSecret(authentication.auth, AUTH_ALIAS, AUTH_FILENAME, DATE_FORMATTER.parse(authentication.sessionExpire))
                            encryptSecret(user, USER_ALIAS, USER_FILENAME, oneYearDate.time)
                            encryptSecret(password, USER_ALIAS, PASSWORD_FILENAME, oneYearDate.time)
                            Observable.just(true)
                        }
                        else -> {
                            Observable.just(false)
                        }
                    }
                }
    }

    fun extendsSession(): Observable<Boolean> =
            if (!isConnected()) {
                Observable.just(false)
            } else if (keyStore.containsAlias(AUTH_ALIAS) && isDataValid(AUTH_ALIAS)) {
                val authToken = decryptSecret(AUTH_ALIAS, AUTH_FILENAME)
                connection
                        .ping(authToken)
                        .flatMap { ping ->
                            if (ping.error.code == 0) {
                                encryptSecret(authToken, AUTH_ALIAS, AUTH_FILENAME,
                                        try {
                                            DATE_FORMATTER.parse(ping.sessionExpire)

                                        } catch (exception: ParseException) {
                                            val date = Date()
                                            date.time += THIRTY_MINUTES
                                            date
                                        }
                                )//todo if empty string ?
                            }

                            Observable.just(ping.error.code == 0)
                        }
            } else {
                authenticate(decryptSecret(USER_ALIAS, USER_FILENAME), decryptSecret(USER_ALIAS, PASSWORD_FILENAME))
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
        val encoded = Base64.encode(secret.toByteArray(), Base64.DEFAULT)
        cipherOutputStream.write(encoded)
        cipherOutputStream.close()
    }

    private fun decryptSecret(alias: String, filename: String): String {
        val privateKeyEntry = getRsaKey(alias)
        val cipher = Cipher.getInstance(RSA_CIPHER)
        cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)

        val inputStream = FileInputStream(dataDirectoryPath + filename)
        val cipherInputStream = CipherInputStream(inputStream, cipher)
        val bytes = ByteArray(1000) // TODO: dynamically resize as we get more data

        var index = 0
        var nextByte = 0
        while (nextByte != -1) {
            bytes[index] = nextByte.toByte()
            index++
            nextByte = cipherInputStream.read()
        }
        cipherInputStream.close()

        val decoded = Base64.decode(bytes, Base64.DEFAULT)
        return String(decoded, Charsets.UTF_8)
    }

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