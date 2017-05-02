package be.florien.ampacheplayer.manager

import android.content.Context
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import be.florien.ampacheplayer.App
import io.reactivex.Observable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.NoSuchPaddingException
import javax.inject.Inject
import javax.security.auth.x500.X500Principal


/**
 * Created by florien on 2/04/17.
 */
class AuthenticationManager {
    /**
     * Constants
     */
    private val ALGORITHM_NAME = "RSA"
    private val KEYSTORE_NAME = "AndroidKeyStore"
    private val USER_FILENAME = "user"
    private val PASSWORD_FILENAME = "password"
    private val AUTH_FILENAME = "auth"

    private val USER_ALIAS = USER_FILENAME
    private val AUTH_ALIAS = "authData"
    private val RSA_CIPHER = "RSA/ECB/NoPadding"
    private val SSL_PROVIDER = "AndroidOpenSSL"
    //private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Fields
     */
    @Inject
    lateinit var connection: AmpacheConnection
    @Inject
    lateinit var context: Context

    val dataDirectoryPath: String
    val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_NAME).apply {
        load(null)
    }

    /**
     * Constructor
     */
    init {
        App.ampacheComponent.inject(this)
        dataDirectoryPath = context.filesDir.absolutePath + File.separator
    }

    /**
     * Public methods
     */
    fun isConnected() = keyStore.containsAlias(AUTH_ALIAS) || keyStore.containsAlias(USER_ALIAS)


    fun authenticate(user: String, password: String): Observable<Boolean> {
        return connection
                .authenticate(user, password)
                .flatMap {
                    authentication ->
                    if (authentication.error.code == 0) {
                        val oneYearDate = Calendar.getInstance()
                        oneYearDate.add(Calendar.YEAR, 1)

                        keyStore.deleteEntry(AUTH_ALIAS)
                        keyStore.deleteEntry(USER_ALIAS)//todo remove when ok

                        if (!keyStore.containsAlias(AUTH_ALIAS)) {//todo if it exist, update the end / problem with date from server
                            createRsaKey(AUTH_ALIAS, oneYearDate.time)//DATE_FORMATTER.parse(authentication.sessionExpire))
                        }
                        encryptSecret(authentication.auth, AUTH_ALIAS, AUTH_FILENAME)

                        if (!keyStore.containsAlias(USER_ALIAS)) {//todo if it exist, update the end ?
                            createRsaKey(USER_ALIAS, oneYearDate.time)
                        }
                        encryptSecret(user, USER_ALIAS, USER_FILENAME)
                        encryptSecret(password, USER_ALIAS, PASSWORD_FILENAME)
                        Observable.just(true)
                    } else {
                        Observable.just(false)
                    }
                }
    }

    fun extendsSession(): Observable<Boolean> = if (!isConnected()) {
        Observable.just(false)
    } else if (keyStore.containsAlias(AUTH_ALIAS)) {
        connection
                .ping(decryptSecret(AUTH_ALIAS, AUTH_FILENAME))
                .flatMap { authentication -> Observable.just(authentication.error.code == 0) } //todo extends session
    } else {
        authenticate(decryptSecret(USER_ALIAS, USER_FILENAME), decryptSecret(USER_ALIAS, PASSWORD_FILENAME))
    }

    /**
     * Private methods
     */
    private fun createRsaKey(alias: String, expiration: Date) {
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
    }

    private fun getRsaKey(alias: String): KeyStore.PrivateKeyEntry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry

    @Throws(KeyStoreException::class, UnrecoverableEntryException::class, NoSuchAlgorithmException::class, NoSuchProviderException::class, NoSuchPaddingException::class, InvalidKeyException::class, IOException::class)
    private fun encryptSecret(secret: String, alias: String, filename: String) {
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

}