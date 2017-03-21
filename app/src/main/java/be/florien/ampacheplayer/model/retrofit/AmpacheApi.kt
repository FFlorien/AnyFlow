package be.florien.ampacheplayer.model.retrofit

import be.florien.ampacheplayer.model.data.Authentication
import be.florien.ampacheplayer.model.data.SongList
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by florien on 10/03/17.
 */
interface AmpacheApi {
    @GET("server/xml.server.php")
    fun authenticate(
            @Query("action") action: String = "handshake",
            @Query("timestamp") time: String,
            @Query("version") version: String = "380001",
            @Query("auth") auth: String,
            @Query("user") limit: String)
            : Observable<Authentication>

    @GET("server/xml.server.php")
    fun getSongs(
            @Query("action") action :String= "songs",
            @Query("auth") auth :String)
    :Observable<SongList>

    @GET("server/xml.server.php")
    fun getSong(
            @Query("auth") auth :String,
            @Query("action") action :String= "song",
            @Query("filter") uid : Long = 0)
    :Observable<SongList>
}