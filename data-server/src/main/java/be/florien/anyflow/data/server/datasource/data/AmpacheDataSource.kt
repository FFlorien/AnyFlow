package be.florien.anyflow.data.server.datasource.data

import be.florien.anyflow.data.server.NetApiError
import be.florien.anyflow.data.server.NetResult
import be.florien.anyflow.data.server.NetSuccess
import be.florien.anyflow.data.server.NetThrowable
import be.florien.anyflow.data.server.di.ServerScope
import be.florien.anyflow.data.server.model.AmpacheAlbumResponse
import be.florien.anyflow.data.server.model.AmpacheApiListResponse
import be.florien.anyflow.data.server.model.AmpacheApiResponse
import be.florien.anyflow.data.server.model.AmpacheArtistResponse
import be.florien.anyflow.data.server.model.AmpacheDeletedSongIdResponse
import be.florien.anyflow.data.server.model.AmpacheGenreResponse
import be.florien.anyflow.data.server.model.AmpachePlaylistResponse
import be.florien.anyflow.data.server.model.AmpachePlaylistsWithSongsResponse
import be.florien.anyflow.data.server.model.AmpacheSongResponse
import be.florien.anyflow.data.server.toNetResult
import be.florien.anyflow.logging.eLog
import be.florien.anyflow.utils.TimeOperations
import retrofit2.Retrofit
import java.util.Calendar
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
