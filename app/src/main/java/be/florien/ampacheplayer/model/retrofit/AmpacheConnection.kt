package be.florien.ampacheplayer.model.retrofit

import be.florien.ampacheplayer.model.data.Authentication
import be.florien.ampacheplayer.model.data.SongList
import com.facebook.stetho.okhttp3.StethoInterceptor
import io.reactivex.Observable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

/**
 * Created by florien on 10/03/17.
 */

class AmpacheConnection {
    private val ampacheApi: AmpacheApi
    var authToken: String = ""

    init {


        val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .build()
        val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.1.42/ampache/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build()

        ampacheApi = retrofit.create(AmpacheApi::class.java)
    }

    fun authenticate(user: String, password: String): Observable<Authentication> {
        val encoder = MessageDigest.getInstance("SHA-256")
        var time = (Date().time / 1000).toString()
        encoder.reset()
        val passwordEncoded = binToHex(encoder.digest(password.toByteArray())).toLowerCase()
        encoder.reset()
        val auth = binToHex(encoder.digest((time + passwordEncoded).toByteArray())).toLowerCase()
        return ampacheApi.authenticate(limit = user, auth = auth, time = time)
    }

    fun getSongs(): Observable<SongList> {
        return ampacheApi.getSongs(auth = authToken)
    }

    fun getSong(): Observable<SongList> {
        return ampacheApi.getSong(authToken)
    }

    internal fun binToHex(data: ByteArray): String {
        return String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
    }
}