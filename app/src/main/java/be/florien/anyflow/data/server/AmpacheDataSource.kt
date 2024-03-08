package be.florien.anyflow.data.server

import android.content.Context
import android.graphics.Bitmap
import be.florien.anyflow.data.TimeOperations
import be.florien.anyflow.data.server.model.*
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ServerScope
import com.bumptech.glide.request.FutureTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import java.util.*
import javax.inject.Inject
import javax.inject.Named

/**
 * Manager for the ampache API server-side
 */
@ServerScope
open class AmpacheDataSource
@Inject constructor(
    @Named("authenticated") private val retrofit: Retrofit
) {
    private val ampacheDataApi = retrofit.create(AmpacheDataApi::class.java)

    /**
     * API calls : data
     */

    suspend fun getNewSongs(offset: Int, limit: Int): NetResult<AmpacheSongResponse> =
        getNetResult(AmpacheDataApi::getNewSongs, offset, limit)

    suspend fun getNewGenres(offset: Int, limit: Int): NetResult<AmpacheGenreResponse> =
        getNetResult(AmpacheDataApi::getNewGenres, offset, limit)

    suspend fun getNewArtists(offset: Int, limit: Int): NetResult<AmpacheArtistResponse> =
        getNetResult(AmpacheDataApi::getNewArtists, offset, limit)

    suspend fun getNewAlbums(offset: Int, limit: Int): NetResult<AmpacheAlbumResponse> =
        getNetResult(AmpacheDataApi::getNewAlbums, offset, limit)

    suspend fun getPlaylists(): NetResult<AmpachePlaylistResponse> =
        getNetResult(AmpacheDataApi::getPlaylists)

    suspend fun getPlaylistsWithSongs(): NetResult<AmpachePlaylistsWithSongsResponse> =
        getNetResult(AmpacheDataApi::getPlaylistsSongs)

    suspend fun getAddedSongs(
        offset: Int,
        limit: Int,
        from: Calendar
    ): NetResult<AmpacheSongResponse> =
        getUpdatedNetResult(AmpacheDataApi::getAddedSongs, offset, limit, from)

    suspend fun getAddedGenres(
        offset: Int,
        limit: Int,
        from: Calendar
    ): NetResult<AmpacheGenreResponse> =
        getUpdatedNetResult(AmpacheDataApi::getAddedGenres, offset, limit, from)

    suspend fun getAddedArtists(
        offset: Int,
        limit: Int,
        from: Calendar
    ): NetResult<AmpacheArtistResponse> =
        getUpdatedNetResult(AmpacheDataApi::getAddedArtists, offset, limit, from)

    suspend fun getAddedAlbums(
        offset: Int,
        limit: Int,
        from: Calendar
    ): NetResult<AmpacheAlbumResponse> =
        getUpdatedNetResult(AmpacheDataApi::getAddedAlbums, offset, limit, from)

    /**
     * API calls : updated data
     */

    suspend fun getUpdatedSongs(
        offset: Int,
        limit: Int,
        from: Calendar
    ): NetResult<AmpacheSongResponse> =
        getUpdatedNetResult(AmpacheDataApi::getUpdatedSongs, offset, limit, from)

    suspend fun getUpdatedGenres(
        offset: Int,
        limit: Int,
        from: Calendar
    ): NetResult<AmpacheGenreResponse> =
        getUpdatedNetResult(AmpacheDataApi::getUpdatedGenres, offset, limit, from)

    suspend fun getUpdatedArtists(
        offset: Int,
        limit: Int,
        from: Calendar
    ): NetResult<AmpacheArtistResponse> =
        getUpdatedNetResult(AmpacheDataApi::getUpdatedArtists, offset, limit, from)

    suspend fun getUpdatedAlbums(
        offset: Int,
        limit: Int,
        from: Calendar
    ): NetResult<AmpacheAlbumResponse> =
        getUpdatedNetResult(AmpacheDataApi::getUpdatedAlbums, offset, limit, from)

    suspend fun getDeletedSongs(offset: Int, limit: Int): NetResult<AmpacheDeletedSongIdResponse> =
        getNetResult(AmpacheDataApi::getDeletedSongs, offset, limit)

    suspend fun getWaveFormImage(songId: Long, context: Context) = withContext(Dispatchers.IO) {
        val serverUrl = retrofit.baseUrl()
        val url = "$serverUrl/waveform.php?song_id=$songId"
        val futureTarget: FutureTarget<Bitmap> = GlideApp.with(context)
            .asBitmap()
            .load(url)
            .submit()

        val bitmap: Bitmap = futureTarget.get()
        GlideApp.with(context).clear(futureTarget)
        bitmap
    }

    suspend fun getStreamError(songId: Long) =
        ampacheDataApi.streamError(songId = songId)

    fun getSongUrl(id: Long): String {
        val serverUrl = retrofit.baseUrl()
        return "${serverUrl}server/json.server.php?action=stream&type=song&id=$id&uid=1"
    }

    fun getArtUrl(type: String, id: Long): String {
        val serverUrl = retrofit.baseUrl()
        return "${serverUrl}server/json.server.php?action=get_art&type=$type&id=$id"
    }

    private suspend fun <T : AmpacheApiResponse> getNetResult(
        apiMethod: suspend AmpacheDataApi.(Int, Int) -> T,
        offset: Int,
        limit: Int
    ): NetResult<T> = try {
        ampacheDataApi.apiMethod(limit, offset).toNetResult()
    } catch (ex: Exception) {
        eLog(ex)
        NetThrowable(ex)
    }

    private suspend fun <T : AmpacheApiResponse> getNetResult(
        apiMethod: suspend AmpacheDataApi.() -> T
    ): NetResult<T> = try {
        ampacheDataApi.apiMethod().toNetResult()
    } catch (ex: Exception) {
        eLog(ex)
        NetThrowable(ex)
    }

    private fun <T : AmpacheApiResponse> T.toNetResult(): NetResult<T> =
        if (error == null) {
            NetSuccess(this)
        } else {
            NetApiError(error)
        }

    private suspend fun <V, T : AmpacheApiListResponse<V>> getUpdatedNetResult(
        apiMethod: suspend AmpacheDataApi.(Int, Int, String) -> T,
        offset: Int,
        limit: Int,
        from: Calendar
    ): NetResult<T> = try {
        ampacheDataApi
            .apiMethod(limit, offset, TimeOperations.getAmpacheCompleteFormatted(from))
            .let {
                if (it.error == null) {
                    NetSuccess(it)
                } else {
                    NetApiError(it.error)
                }
            }
    } catch (ex: Exception) {
        eLog(ex)
        NetThrowable(ex)
    }
}
