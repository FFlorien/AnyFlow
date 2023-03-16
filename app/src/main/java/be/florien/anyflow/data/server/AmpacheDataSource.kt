package be.florien.anyflow.data.server

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.TimeOperations
import be.florien.anyflow.data.server.model.*
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.extension.applyPutInt
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ServerScope
import com.bumptech.glide.request.FutureTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import java.util.*
import javax.inject.Inject

/**
 * Manager for the ampache API server-side
 */
@ServerScope
open class AmpacheDataSource
@Inject constructor(
    private val retrofit: Retrofit,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val OFFSET_ADD_SONG = "OFFSET_ADD_SONG"
        private const val OFFSET_ADD_GENRE = "OFFSET_ADD_GENRE"
        private const val OFFSET_ADD_ARTIST = "OFFSET_ADD_ARTIST"
        private const val OFFSET_ADD_ALBUM = "OFFSET_ADD_ALBUM"
        private const val OFFSET_PLAYLIST = "OFFSET_PLAYLIST"
        private const val OFFSET_UPDATE_SONG = "OFFSET_UPDATE_SONG"
        private const val OFFSET_UPDATE_GENRE = "OFFSET_UPDATE_GENRE"
        private const val OFFSET_UPDATE_ARTIST = "OFFSET_UPDATE_ARTIST"
        private const val OFFSET_UPDATE_ALBUM = "OFFSET_UPDATE_ALBUM"
        private const val OFFSET_DELETED_SONGS = "OFFSET_DELETED_SONGS"
        private const val COUNT_SONGS = "COUNT_SONGS"
        private const val COUNT_GENRES = "COUNT_GENRES"
        private const val COUNT_ALBUMS = "COUNT_ALBUMS"
        private const val COUNT_ARTIST = "COUNT_ARTIST"
        private const val COUNT_PLAYLIST = "COUNT_PLAYLIST"
        const val SERVER_UPDATE = "SERVER_UPDATE"
        const val SERVER_ADD = "SERVER_ADD"
        const val SERVER_CLEAN = "SERVER_CLEAN"
    }

    private val itemLimit: Int = 250
    private val ampacheDataApi = retrofit.create(AmpacheDataApi::class.java)

    val songsPercentageUpdater = MutableLiveData(-1)
    val genresPercentageUpdater = MutableLiveData(-1)
    val artistsPercentageUpdater = MutableLiveData(-1)
    val albumsPercentageUpdater = MutableLiveData(-1)
    val playlistsPercentageUpdater = MutableLiveData(-1)

    /**
     * API calls : data
     */

    fun getNewSongs(): Flow<List<AmpacheSong>> = flow {
        var list = getNewItems(AmpacheDataApi::getNewSongs, OFFSET_ADD_SONG)
        while (list != null && list.song.isNotEmpty()) {
            updateRetrievingData(
                list.song,
                OFFSET_ADD_SONG,
                COUNT_SONGS,
                songsPercentageUpdater
            )
            emit(list.song)
            list = getNewItems(AmpacheDataApi::getNewSongs, OFFSET_ADD_SONG)
        }
    }


    fun getNewGenres(): Flow<List<AmpacheNameId>> = flow {
        var list = getNewItems(AmpacheDataApi::getNewGenres, OFFSET_ADD_GENRE)
        while (list != null && list.genre.isNotEmpty()) {
            updateRetrievingData(
                list.genre,
                OFFSET_ADD_GENRE,
                COUNT_GENRES,
                genresPercentageUpdater
            )
            emit(list.genre)
            list = getNewItems(AmpacheDataApi::getNewGenres, OFFSET_ADD_GENRE)
        }
    }

    fun getNewArtists(): Flow<List<AmpacheArtist>> = flow {
        var list = getNewItems(AmpacheDataApi::getNewArtists, OFFSET_ADD_ARTIST)
        while (list != null && list.artist.isNotEmpty()) {
            updateRetrievingData(
                list.artist,
                OFFSET_ADD_ARTIST,
                COUNT_ARTIST,
                artistsPercentageUpdater
            )
            emit(list.artist)
            list = getNewItems(AmpacheDataApi::getNewArtists, OFFSET_ADD_ARTIST)
        }
    }

    fun getNewAlbums(): Flow<List<AmpacheAlbum>> = flow {
        var list = getNewItems(AmpacheDataApi::getNewAlbums, OFFSET_ADD_ALBUM)
        while (list != null && list.album.isNotEmpty()) {
            updateRetrievingData(
                list.album,
                OFFSET_ADD_ALBUM,
                COUNT_ALBUMS,
                albumsPercentageUpdater
            )
            emit(list.album)
            list = getNewItems(AmpacheDataApi::getNewAlbums, OFFSET_ADD_ALBUM)
        }
    }

    fun getPlaylists(): Flow<List<AmpachePlayListWithSongs>> = flow {
        var list = getNewItems(AmpacheDataApi::getPlaylists, OFFSET_PLAYLIST)
        while (list != null && list.playlist.isNotEmpty()) {
            updateRetrievingData(
                list.playlist,
                OFFSET_PLAYLIST,
                COUNT_PLAYLIST,
                playlistsPercentageUpdater
            )
            emit(list.playlist)
            list = getNewItems(AmpacheDataApi::getPlaylists, OFFSET_PLAYLIST)
        }
    }

    fun getAddedSongs(from: Calendar): Flow<List<AmpacheSong>> = flow {
        var list = getItems(AmpacheDataApi::getAddedSongs, OFFSET_ADD_SONG, from)
        while (list != null && list.song.isNotEmpty()) {
            updateRetrievingData(
                list.song,
                OFFSET_ADD_SONG,
                COUNT_SONGS,
                songsPercentageUpdater
            )
            emit(list.song)
            list = getItems(AmpacheDataApi::getAddedSongs, OFFSET_ADD_SONG, from)
        }
    }

    fun getAddedGenres(from: Calendar): Flow<List<AmpacheNameId>> = flow {
        var list = getItems(AmpacheDataApi::getAddedGenres, OFFSET_ADD_GENRE, from)
        while (list != null && list.genre.isNotEmpty()) {
            updateRetrievingData(
                list.genre,
                OFFSET_ADD_GENRE,
                COUNT_GENRES,
                genresPercentageUpdater
            )
            emit(list.genre)
            list = getItems(AmpacheDataApi::getAddedGenres, OFFSET_ADD_GENRE, from)
        }
    }

    fun getAddedArtists(from: Calendar): Flow<List<AmpacheArtist>> = flow {
        var list = getItems(AmpacheDataApi::getAddedArtists, OFFSET_ADD_ARTIST, from)
        while (list != null && list.artist.isNotEmpty()) {
            updateRetrievingData(
                list.artist,
                OFFSET_ADD_ARTIST,
                COUNT_ARTIST,
                artistsPercentageUpdater
            )
            emit(list.artist)
            list = getItems(AmpacheDataApi::getAddedArtists, OFFSET_ADD_ARTIST, from)
        }
    }

    fun getAddedAlbums(from: Calendar): Flow<List<AmpacheAlbum>> = flow {
        var list = getItems(AmpacheDataApi::getAddedAlbums, OFFSET_ADD_ALBUM, from)
        while (list != null && list.album.isNotEmpty()) {
            updateRetrievingData(
                list.album,
                OFFSET_ADD_ALBUM,
                COUNT_ALBUMS,
                albumsPercentageUpdater
            )
            emit(list.album)
            list = getItems(AmpacheDataApi::getAddedAlbums, OFFSET_ADD_ALBUM, from)
        }
    }

    /**
     * API calls : updated data
     */

    fun getUpdatedSongs(from: Calendar): Flow<List<AmpacheSong>> = flow {
        var list = getItems(AmpacheDataApi::getUpdatedSongs, OFFSET_UPDATE_SONG, from)
        while (list != null && list.song.isNotEmpty()) {
            updateRetrievingData(
                list.song,
                OFFSET_UPDATE_SONG,
                COUNT_SONGS,
                songsPercentageUpdater
            )
            emit(list.song)
            list = getItems(AmpacheDataApi::getUpdatedSongs, OFFSET_UPDATE_SONG, from)
        }
    }

    fun getUpdatedGenres(from: Calendar): Flow<List<AmpacheNameId>> = flow {
        var list = getItems(AmpacheDataApi::getUpdatedGenres, OFFSET_UPDATE_GENRE, from)
        while (list != null && list.genre.isNotEmpty()) {
            updateRetrievingData(
                list.genre,
                OFFSET_UPDATE_GENRE,
                COUNT_GENRES,
                genresPercentageUpdater
            )
            emit(list.genre)
            list = getItems(AmpacheDataApi::getUpdatedGenres, OFFSET_UPDATE_GENRE, from)
        }
    }

    fun getUpdatedArtists(from: Calendar): Flow<List<AmpacheArtist>> = flow {
        var list = getItems(AmpacheDataApi::getUpdatedArtists, OFFSET_UPDATE_ARTIST, from)
        while (list != null && list.artist.isNotEmpty()) {
            updateRetrievingData(
                list.artist,
                OFFSET_UPDATE_ARTIST,
                COUNT_ARTIST,
                artistsPercentageUpdater
            )
            emit(list.artist)
            list = getItems(AmpacheDataApi::getUpdatedArtists, OFFSET_UPDATE_ARTIST, from)
        }
    }

    fun getUpdatedAlbums(from: Calendar): Flow<List<AmpacheAlbum>> = flow {
        var list = getItems(AmpacheDataApi::getUpdatedAlbums, OFFSET_UPDATE_ALBUM, from)
        while (list != null && list.album.isNotEmpty()) {
            updateRetrievingData(
                list.album,
                OFFSET_UPDATE_ALBUM,
                COUNT_ALBUMS,
                albumsPercentageUpdater
            )
            emit(list.album)
            list = getItems(AmpacheDataApi::getUpdatedAlbums, OFFSET_UPDATE_ALBUM, from)
        }
    }

    suspend fun getDeletedSongs(): List<AmpacheSongId>? {
        var currentOffset = sharedPreferences.getInt(OFFSET_DELETED_SONGS, 0)
        val deletedList =
            ampacheDataApi.getDeletedSongs(

                limit = itemLimit,
                offset = currentOffset
            ).deleted_song
        return if (deletedList.isEmpty()) {
            null
        } else {
            currentOffset += deletedList.size
            sharedPreferences.applyPutInt(OFFSET_DELETED_SONGS, currentOffset)
            deletedList
        }
    }

    suspend fun getWaveFormImage(songId: Long, context: Context) = withContext(Dispatchers.IO) {
        val serverUrl = retrofit.baseUrl()
        val url = "$serverUrl/waveform.php?song_id=$songId"
        val futureTarget: FutureTarget<Bitmap> = GlideApp.with(context)
            .asBitmap()
            .load(url)
            .timeout(60000)//it may need some time to generate those
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

    private suspend fun <T> getItems(
        apiMethod: suspend AmpacheDataApi.(Int, Int, String) -> T?,
        offsetName: String,
        from: Calendar
    ): T? {
        try {
            val currentOffset = sharedPreferences.getInt(offsetName, 0)
            return ampacheDataApi.apiMethod(
                itemLimit,
                currentOffset,
                TimeOperations.getAmpacheCompleteFormatted(from)
            )
        } catch (ex: Exception) {
            eLog(ex)
            throw ex
        }
    }

    private suspend fun <T> getNewItems(
        apiMethod: suspend AmpacheDataApi.(Int, Int) -> T?,
        offsetName: String
    ): T? {
        try {
            val currentOffset = sharedPreferences.getInt(offsetName, 0)
            return ampacheDataApi.apiMethod(
                itemLimit,
                currentOffset
            )
        } catch (ex: Exception) {
            eLog(ex)
            throw ex
        }
    }

    private fun <T> updateRetrievingData(
        itemList: List<T>?,
        offsetName: String,
        countName: String,
        percentageUpdater: MutableLiveData<Int>
    ) {
        var currentOffset = sharedPreferences.getInt(offsetName, 0)
        val totalSongs = sharedPreferences.getInt(countName, 1)
        if (itemList.isNullOrEmpty()) {
            percentageUpdater.postValue(-1)
        } else {
            currentOffset += itemList.size
            val percentage = (currentOffset * 100) / totalSongs
            percentageUpdater.postValue(percentage)
            sharedPreferences.applyPutInt(offsetName, currentOffset)
        }
    }

    fun resetAddOffsets() {
        sharedPreferences.edit().apply {
            remove(OFFSET_ADD_SONG)
            remove(OFFSET_ADD_GENRE)
            remove(OFFSET_ADD_ARTIST)
            remove(OFFSET_ADD_ALBUM)
            remove(OFFSET_PLAYLIST)
        }.apply()
    }

    fun resetUpdateOffsets() {
        sharedPreferences.edit().apply {
            remove(OFFSET_UPDATE_SONG)
            remove(OFFSET_UPDATE_GENRE)
            remove(OFFSET_UPDATE_ARTIST)
            remove(OFFSET_UPDATE_ALBUM)
        }.apply()
    }

    fun resetPlaylistOffsets() {
        sharedPreferences.edit().apply {
            remove(OFFSET_PLAYLIST)
        }.apply()
    }

    fun cancelPercentageUpdaters() {
        genresPercentageUpdater.postValue(-1)
        artistsPercentageUpdater.postValue(-1)
        albumsPercentageUpdater.postValue(-1)
        songsPercentageUpdater.postValue(-1)
        playlistsPercentageUpdater.postValue(-1)
    }
}
