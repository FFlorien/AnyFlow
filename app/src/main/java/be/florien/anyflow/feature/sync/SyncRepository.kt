package be.florien.anyflow.feature.sync

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.TimeOperations
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.server.NetApiError
import be.florien.anyflow.data.server.NetResult
import be.florien.anyflow.data.server.NetSuccess
import be.florien.anyflow.data.server.NetThrowable
import be.florien.anyflow.data.server.model.AmpacheApiResponse
import be.florien.anyflow.data.toDbAlbum
import be.florien.anyflow.data.toDbArtist
import be.florien.anyflow.data.toDbGenre
import be.florien.anyflow.data.toDbPlaylist
import be.florien.anyflow.data.toDbPlaylistSong
import be.florien.anyflow.data.toDbSong
import be.florien.anyflow.data.toDbSongGenres
import be.florien.anyflow.data.toDbSongId
import be.florien.anyflow.extension.applyPutLong
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ServerScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

/**
 * Update the local data with the one from the server
 */
@ServerScope
class SyncRepository
@Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val ampacheDataSource: AmpacheDataSource,
    private val sharedPreferences: SharedPreferences
) {
    val songsPercentageUpdater = MutableLiveData(-1)
    val genresPercentageUpdater = MutableLiveData(-1)
    val artistsPercentageUpdater = MutableLiveData(-1)
    val albumsPercentageUpdater = MutableLiveData(-1)
    val playlistsPercentageUpdater = MutableLiveData(-1)

    private val changeUpdater: LiveData<Int?> = MutableLiveData()

    /**
     * Getter with server updates
     */

    /*
        todo: The following todo was done before a refactoring, see if it is still relevant
        todo:
        - First: check if it work, and verify what the deal is with Caravan Palace - Caravan Palace
        - remove *addAll* and updateAll(edit: we have to keep updateAll for tag edits): it didn't worked well...
        - Check how it works with genre, and if it works:
        - Clean deleted songs (and unused album/artist ?)
        - add a method to verify by browsing
            - See the number of Songs, artists, albums by genre
            - check with database
            - if there's any difference
                - either the difference between DB and Server is below a defined treshold, then get the filtered data immediately (E.G: 125 songs in db and 145 on server)
                - or it's above the threshold (2467 in db, 3367 on server) and we browse down
     */

    suspend fun syncAll() {
        if (libraryDatabase.getSongDao().songCount() == 0) {
            getFromScratch()
        } else {
            addAll()
            updateAll()
            cleanAll()
        }
        playlists()
        resetOffsets()
        cancelPercentageUpdaters()
    }

    private suspend fun getFromScratch() = withContext(Dispatchers.IO) {
        newGenres()
        newArtists()
        newAlbums()
        newSongs()
        playlists()
        val currentMillis = TimeOperations.getCurrentDate().timeInMillis
        sharedPreferences.edit().apply {
            putLong(LAST_ADD_QUERY, currentMillis)
            putLong(LAST_UPDATE_QUERY, currentMillis)
            putLong(LAST_CLEAN_QUERY, currentMillis)
        }.apply()
    }

    private suspend fun addAll() =
        sync(LAST_ADD_QUERY) { lastSync ->
            addGenres(lastSync)
            addArtists(lastSync)
            addAlbums(lastSync)
            addSongs(lastSync)
        }

    private suspend fun updateAll() =
        sync(LAST_UPDATE_QUERY) { lastSync ->
            updateGenres(lastSync)
            updateArtists(lastSync)
            updateAlbums(lastSync)
            updateSongs(lastSync)
        }

    private suspend fun cleanAll() =
        sync(LAST_CLEAN_QUERY) {
            updateDeletedSongs()
        }

    private suspend fun sync(
        lastDbSyncName: String,
        sync: suspend (Calendar) -> Unit
    ) = withContext(Dispatchers.IO) {
        val nowDate = TimeOperations.getCurrentDate()
        val lastSyncMillis = sharedPreferences.getLong(lastDbSyncName, 0L)
        val lastSync = TimeOperations.getDateFromMillis(lastSyncMillis)
        sync(lastSync)
        sharedPreferences.applyPutLong(lastDbSyncName, nowDate.timeInMillis)
    }

    /**
     * Private Method : New data
     */

    private suspend fun newGenres() =
        getNewData(
            OFFSET_GENRE,
            genresPercentageUpdater,
            AmpacheDataSource::getNewGenres
        ) { success ->
            asyncUpdate(CHANGE_GENRES) {
                libraryDatabase.getGenreDao().upsert(success.data.list.map { it.toDbGenre() })
            }
        }

    private suspend fun newArtists() =
        getNewData(
            OFFSET_ARTIST,
            artistsPercentageUpdater,
            AmpacheDataSource::getNewArtists
        ) { success ->
            asyncUpdate(CHANGE_ARTISTS) {
                libraryDatabase.getArtistDao().upsert(success.data.list.map { it.toDbArtist() })
            }
        }

    private suspend fun newAlbums() =
        getNewData(
            OFFSET_ALBUM,
            albumsPercentageUpdater,
            AmpacheDataSource::getNewAlbums
        ) { success ->
            asyncUpdate(CHANGE_ALBUMS) {
                libraryDatabase.getAlbumDao().upsert(success.data.list.map { it.toDbAlbum() })
            }
        }

    private suspend fun newSongs() =
        getNewData(OFFSET_SONG, songsPercentageUpdater, AmpacheDataSource::getNewSongs) { success ->
            asyncUpdate(CHANGE_SONGS) {
                libraryDatabase.getSongDao().upsert(success.data.list.map { it.toDbSong() })
                val songGenres = success.data.list.map { it.toDbSongGenres() }.flatten()
                libraryDatabase.getSongGenreDao().upsert(songGenres)
            }
        }

    suspend fun playlists() {
        //todo review this methods, suspicious: how does it handle deleted playlists and deleted from playlist ?
        getNewData(
            OFFSET_PLAYLIST,
            playlistsPercentageUpdater,
            AmpacheDataSource::getPlaylists
        ) { success ->
            asyncUpdate(CHANGE_PLAYLISTS) {
                libraryDatabase.getPlaylistDao()
                    .upsert(success.data.list.map { it.toDbPlaylist() })

                for (playlist in success.data.list) {
                    libraryDatabase.getPlaylistSongsDao().deleteSongsFromPlaylist(playlist.id)
                    libraryDatabase.getPlaylistSongsDao().upsert(playlist.items.map {
                        it.toDbPlaylistSong(playlist.id)
                    })
                }
            }
        }
    }

    /**
     * Private Method : added data
     */

    private suspend fun addGenres(from: Calendar) =
        getUpdatedData(
            OFFSET_GENRE,
            genresPercentageUpdater,
            from,
            AmpacheDataSource::getAddedGenres
        ) { success ->
            asyncUpdate(CHANGE_GENRES) {
                libraryDatabase.getGenreDao().upsert(success.data.list.map { it.toDbGenre() })
            }
        }

    private suspend fun addArtists(from: Calendar) =
        getUpdatedData(
            OFFSET_ARTIST,
            artistsPercentageUpdater,
            from,
            AmpacheDataSource::getAddedArtists
        ) { success ->
            asyncUpdate(CHANGE_ARTISTS) {
                libraryDatabase.getArtistDao().upsert(success.data.list.map { it.toDbArtist() })
            }
        }

    private suspend fun addAlbums(from: Calendar) =
        getUpdatedData(
            OFFSET_ALBUM,
            albumsPercentageUpdater,
            from,
            AmpacheDataSource::getAddedAlbums
        ) { success ->
            asyncUpdate(CHANGE_ALBUMS) {
                libraryDatabase.getAlbumDao().upsert(success.data.list.map { it.toDbAlbum() })
            }
        }

    private suspend fun addSongs(from: Calendar) =
        getUpdatedData(
            OFFSET_SONG,
            songsPercentageUpdater,
            from,
            AmpacheDataSource::getAddedSongs
        ) { success ->
            asyncUpdate(CHANGE_SONGS) {
                libraryDatabase.getSongDao().upsert(success.data.list.map { it.toDbSong() })

                val songGenres = success.data.list.map { it.toDbSongGenres() }.flatten()
                libraryDatabase.getSongGenreDao().upsert(songGenres)
            }
        }

    /**
     * Private Method : Updated data
     */

    private suspend fun updateGenres(from: Calendar) =
        getUpdatedData(
            OFFSET_GENRE,
            genresPercentageUpdater,
            from,
            AmpacheDataSource::getUpdatedGenres
        ) { success ->
            asyncUpdate(CHANGE_GENRES) {
                libraryDatabase.getGenreDao().upsert(success.data.list.map { it.toDbGenre() })
            }
        }

    private suspend fun updateArtists(from: Calendar) =
        getUpdatedData(
            OFFSET_ARTIST,
            artistsPercentageUpdater,
            from,
            AmpacheDataSource::getUpdatedArtists
        ) { success ->
            asyncUpdate(CHANGE_ARTISTS) {
                libraryDatabase.getArtistDao().upsert(success.data.list.map { it.toDbArtist() })
            }
        }

    private suspend fun updateAlbums(from: Calendar) =
        getUpdatedData(
            OFFSET_ALBUM,
            albumsPercentageUpdater,
            from,
            AmpacheDataSource::getUpdatedAlbums
        ) { success ->
            asyncUpdate(CHANGE_ALBUMS) {
                libraryDatabase.getAlbumDao().upsert(success.data.list.map { it.toDbAlbum() })
            }
        }

    private suspend fun updateSongs(from: Calendar) =
        getUpdatedData(
            OFFSET_SONG,
            songsPercentageUpdater,
            from,
            AmpacheDataSource::getUpdatedSongs
        ) { success ->
            val songIds = success.data.list.map { it.id }
            val songsToUpdate = libraryDatabase.getSongDao().songsToUpdate(songIds)
            val songs = success.data.list.map { new ->
                val localUri = songsToUpdate.firstOrNull { old -> old.id == new.id }?.local
                new.toDbSong(localUri)
            }
            asyncUpdate(CHANGE_SONGS) {
                libraryDatabase.getSongDao().upsert(songs)
                val songGenres = success.data.list.map { it.toDbSongGenres() }.flatten()
                libraryDatabase.getSongGenreDao().upsert(songGenres)
            }
        }

    private suspend fun updateDeletedSongs() =
        getNewData(
            OFFSET_SONG,
            songsPercentageUpdater,
            AmpacheDataSource::getDeletedSongs
        ) { success ->
            libraryDatabase.getSongDao()
                .deleteWithId(success.data.list.map { it.toDbSongId() })
        }

    private suspend fun <V, T : AmpacheApiResponse<V>> getNewData(
        offsetKey: String,
        percentageUpdater: MutableLiveData<Int>,
        getFromApi: suspend AmpacheDataSource.(Int, Int) -> NetResult<T>,
        updateDb: suspend (NetSuccess<T>) -> Unit
    ) {
        getData(
            offsetKey,
            percentageUpdater,
            { offset, limit -> ampacheDataSource.getFromApi(offset, limit) },
            updateDb
        )
    }

    private suspend fun <V, T : AmpacheApiResponse<V>> getUpdatedData(
        offsetKey: String,
        percentageUpdater: MutableLiveData<Int>,
        calendar: Calendar,
        getFromApi: suspend AmpacheDataSource.(Int, Int, Calendar) -> NetResult<T>,
        updateDb: suspend (NetSuccess<T>) -> Unit
    ) {
        getData(
            offsetKey,
            percentageUpdater,
            { offset, limit -> ampacheDataSource.getFromApi(offset, limit, calendar) },
            updateDb
        )
    }

    private suspend fun <V, T : AmpacheApiResponse<V>> getData(
        offsetKey: String,
        percentageUpdater: MutableLiveData<Int>,
        getFromApi: suspend (Int, Int) -> NetResult<T>,
        updateDb: suspend (NetSuccess<T>) -> Unit
    ) {
        var offset = sharedPreferences.getInt(offsetKey, 0)
        var count = Int.MAX_VALUE
        var limit = ITEM_LIMIT
        var result = getFromApi(offset, limit)
        while (offset < count) {
            when (result) {
                is NetSuccess -> {
                    count = result.data.total_count
                    updateDb(result)
                    offset += result.data.list.size
                    sharedPreferences.edit().putInt(offsetKey, offset).apply()
                }

                is NetApiError -> { //todo better handling of ALL error codes
                    when (limit) {
                        ITEM_LIMIT -> limit = 10
                        10 -> limit = 1
                        else -> { //todo display error to user
                            offset += 1
                            limit = ITEM_LIMIT
                        }
                    }
                }

                is NetThrowable -> {
                    eLog(result.throwable, "Encountered exception during syncing for $offsetKey")
                    break
                }
            }
            val percentage = (offset * 100) / count
            percentageUpdater.postValue(percentage)
            result = getFromApi(offset, limit)
        }
    }

    private suspend fun asyncUpdate(changeSubject: Int, action: suspend () -> Unit) {
        MainScope().launch {
            (changeUpdater as MutableLiveData).value = changeSubject
        }
        action()
        MainScope().launch {
            (changeUpdater as MutableLiveData).value = null
        }
    }

    private fun resetOffsets() {
        sharedPreferences.edit().apply {
            remove(OFFSET_SONG)
            remove(OFFSET_GENRE)
            remove(OFFSET_ARTIST)
            remove(OFFSET_ALBUM)
            remove(OFFSET_PLAYLIST)
        }.apply()
    }

    private fun cancelPercentageUpdaters() {
        genresPercentageUpdater.postValue(-1)
        artistsPercentageUpdater.postValue(-1)
        albumsPercentageUpdater.postValue(-1)
        songsPercentageUpdater.postValue(-1)
        playlistsPercentageUpdater.postValue(-1)
    }

    companion object {
        const val CHANGE_SONGS = 0
        const val CHANGE_ARTISTS = 1
        const val CHANGE_ALBUMS = 2
        const val CHANGE_GENRES = 3
        const val CHANGE_PLAYLISTS = 4

        private const val ITEM_LIMIT: Int = 250

        private const val LAST_ADD_QUERY = "LAST_ADD_QUERY"
        private const val LAST_UPDATE_QUERY = "LAST_UPDATE_QUERY"
        private const val LAST_CLEAN_QUERY = "LAST_CLEAN_QUERY"

        const val ART_TYPE_SONG = "song"
        const val ART_TYPE_ALBUM = "album"
        const val ART_TYPE_ARTIST = "artist"
        const val ART_TYPE_PLAYLIST = "playlist"

        private const val OFFSET_SONG = "OFFSET_SONG"
        private const val OFFSET_GENRE = "OFFSET_GENRE"
        private const val OFFSET_ARTIST = "OFFSET_ARTIST"
        private const val OFFSET_ALBUM = "OFFSET_ALBUM"
        private const val OFFSET_PLAYLIST = "OFFSET_PLAYLIST"
    }
}