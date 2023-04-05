package be.florien.anyflow.data

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.extension.applyPutLong
import be.florien.anyflow.injection.ServerScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
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


    val changeUpdater: LiveData<Int?> = MutableLiveData()

    /**
     * Getter with server updates
     */

    /*
        todo:
        - First: check if it work, and verify what the deal is with Caravan Palace - Caravan Palace
        - remove addAll and updateAll: it didn't worked well...
        - Check how it works with genre, and if it works:
        - Clean deleted songs (and unused album/artist ?)
        - add a method to verify by browsing
            - See the number of Songs, artists, albums by genre
            - check with database
            - if there's any difference
                - either the difference between DB and Server is below a defined treshold, then get the filtered data immediately (E.G: 125 songs in db and 145 on server)
                - or it's above the treshold (2467 in db, 3367 on server) and we browse down
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
    }

    private suspend fun getFromScratch() = withContext(Dispatchers.IO) {
        newGenres()
        newArtists()
        newAlbums()
        newSongs()
        playlists()
        val currentMillis = TimeOperations.getCurrentDate().timeInMillis
        sharedPreferences.edit().apply {
            putLong(AmpacheDataSource.SERVER_ADD, currentMillis)
            putLong(AmpacheDataSource.SERVER_UPDATE, currentMillis)
            putLong(AmpacheDataSource.SERVER_CLEAN, currentMillis)
            putLong(LAST_ADD_QUERY, currentMillis)
            putLong(LAST_UPDATE_QUERY, currentMillis)
            putLong(LAST_CLEAN_QUERY, currentMillis)
        }.apply()
    }

    private suspend fun addAll() =
        syncIfOutdated(AmpacheDataSource.SERVER_ADD, LAST_ADD_QUERY) { lastSync ->
            addGenres(lastSync)
            addArtists(lastSync)
            addAlbums(lastSync)
            addSongs(lastSync)
            ampacheDataSource.resetAddOffsets()
        }

    private suspend fun updateAll() =
        syncIfOutdated(AmpacheDataSource.SERVER_UPDATE, LAST_UPDATE_QUERY) { lastSync ->
            updateGenres(lastSync)
            updateArtists(lastSync)
            updateAlbums(lastSync)
            updateSongs(lastSync)
            ampacheDataSource.resetUpdateOffsets()
        }

    private suspend fun cleanAll() =
        syncIfOutdated(AmpacheDataSource.SERVER_CLEAN, LAST_CLEAN_QUERY) {
            updateDeletedSongs()
        }

    private suspend fun syncIfOutdated(
        lastServerSyncName: String,
        lastDbSyncName: String,
        sync: suspend (Calendar) -> Unit
    ) = withContext(Dispatchers.IO) {
        val nowDate = TimeOperations.getCurrentDate()
        val lastSyncMillis = sharedPreferences.getLong(lastServerSyncName, 0L)
        val lastServerSync = TimeOperations.getDateFromMillis(lastSyncMillis)
        val lastSync =
            TimeOperations.getDateFromMillis(sharedPreferences.getLong(lastDbSyncName, 0L))
        if (lastServerSync.after(lastSync) || lastSyncMillis == 0L) {
            sync(lastSync)
            sharedPreferences.applyPutLong(lastDbSyncName, nowDate.timeInMillis)
        }
    }

    /**
     * Private Method : New data
     */

    private suspend fun newSongs() =
        newList(AmpacheDataSource::getNewSongs) { ampacheSongList ->
            if (ampacheSongList != null) {
                val songs = ampacheSongList.map { it.toDbSong() }
                asyncUpdate(CHANGE_SONGS) {
                    libraryDatabase.getSongDao().upsert(songs)
                    val songGenres = ampacheSongList.map { it.toDbSongGenres() }.flatten()
                    libraryDatabase.getSongGenreDao().upsert(songGenres)
                }
            }
        }

    private suspend fun newGenres() =
        newList(AmpacheDataSource::getNewGenres) { ampacheGenreList ->
            if (ampacheGenreList != null) {
                asyncUpdate(CHANGE_GENRES) {
                    libraryDatabase.getGenreDao().upsert(ampacheGenreList.map { it.toDbGenre() })
                }
            }
        }

    private suspend fun newArtists() =
        newList(AmpacheDataSource::getNewArtists) { ampacheArtistList ->
            if (ampacheArtistList != null) {
                asyncUpdate(CHANGE_ARTISTS) {
                    libraryDatabase.getArtistDao().upsert(ampacheArtistList.map { it.toDbArtist() })
                }
            }
        }

    private suspend fun newAlbums() =
        newList(AmpacheDataSource::getNewAlbums) { ampacheAlbumList ->
            if (ampacheAlbumList != null) {
                asyncUpdate(CHANGE_ALBUMS) {
                    libraryDatabase.getAlbumDao().upsert(ampacheAlbumList.map { it.toDbAlbum() })
                }
            }
        }

    suspend fun playlists() {
        ampacheDataSource.getPlaylists()
            .flowOn(Dispatchers.IO)
            .onEach { playlistList ->
                asyncUpdate(CHANGE_PLAYLISTS) {
                    libraryDatabase.getPlaylistDao().upsert(playlistList.map { it.toDbPlaylist() })

                    for (playlist in playlistList) {
                        libraryDatabase.getPlaylistSongsDao().deleteSongsFromPlaylist(playlist.id)
                        libraryDatabase.getPlaylistSongsDao().upsert(playlist.items.map {
                            it.toDbPlaylistSong(playlist.id)
                        })
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .onCompletion {
                ampacheDataSource.resetPlaylistOffsets()
                ampacheDataSource.cancelPercentageUpdaters()
            }
            .collect()
    }

    /**
     * Private Method : added data
     */

    private suspend fun addSongs(from: Calendar) =
        updateList(from, AmpacheDataSource::getAddedSongs) { ampacheSongList ->
            if (ampacheSongList != null) {
                val songs = ampacheSongList.map { it.toDbSong() }
                asyncUpdate(CHANGE_SONGS) {
                    libraryDatabase.getSongDao().upsert(songs)

                    val songGenres = ampacheSongList.map { it.toDbSongGenres() }.flatten()
                    libraryDatabase.getSongGenreDao().upsert(songGenres)
                }
            }
        }

    private suspend fun addGenres(from: Calendar) =
        updateList(from, AmpacheDataSource::getAddedGenres) { ampacheGenreList ->
            if (ampacheGenreList != null) {
                asyncUpdate(CHANGE_GENRES) {
                    libraryDatabase.getGenreDao().upsert(ampacheGenreList.map { it.toDbGenre() })
                }
            }
        }

    private suspend fun addArtists(from: Calendar) =
        updateList(from, AmpacheDataSource::getAddedArtists) { ampacheArtistList ->
            if (ampacheArtistList != null) {
                asyncUpdate(CHANGE_ARTISTS) {
                    libraryDatabase.getArtistDao().upsert(ampacheArtistList.map { it.toDbArtist() })
                }
            }
        }

    private suspend fun addAlbums(from: Calendar) =
        updateList(from, AmpacheDataSource::getAddedAlbums) { ampacheAlbumList ->
            if (ampacheAlbumList != null) {
                asyncUpdate(CHANGE_ALBUMS) {
                    libraryDatabase.getAlbumDao().upsert(ampacheAlbumList.map { it.toDbAlbum() })
                }
            }
        }

    /**
     * Private Method : Updated data
     */

    private suspend fun updateSongs(from: Calendar) =
        updateList(from, AmpacheDataSource::getUpdatedSongs) { ampacheSongList ->
            if (ampacheSongList != null) {
                val songs = ampacheSongList.map { it.toDbSong() }
                asyncUpdate(CHANGE_SONGS) {
                    libraryDatabase.getSongDao().upsert(songs)
                    val songGenres = ampacheSongList.map { it.toDbSongGenres() }.flatten()
                    libraryDatabase.getSongGenreDao().upsert(songGenres)
                }
            }
        }

    private suspend fun updateGenres(from: Calendar) =
        updateList(from, AmpacheDataSource::getUpdatedGenres) { ampacheGenreList ->
            if (ampacheGenreList != null) {
                asyncUpdate(CHANGE_GENRES) {
                    libraryDatabase.getGenreDao().upsert(ampacheGenreList.map { it.toDbGenre() })
                }
            }
        }

    private suspend fun updateArtists(from: Calendar) =
        updateList(from, AmpacheDataSource::getUpdatedArtists) { ampacheArtistList ->
            if (ampacheArtistList != null) {
                asyncUpdate(CHANGE_ARTISTS) {
                    libraryDatabase.getArtistDao().upsert(ampacheArtistList.map { it.toDbArtist() })
                }
            }
        }

    private suspend fun updateAlbums(from: Calendar) =
        updateList(from, AmpacheDataSource::getUpdatedAlbums) { ampacheAlbumList ->
            if (ampacheAlbumList != null) {
                asyncUpdate(CHANGE_ALBUMS) {
                    libraryDatabase.getAlbumDao().upsert(ampacheAlbumList.map { it.toDbAlbum() })
                }
            }
        }

    private suspend fun updateDeletedSongs() {
        var listOnServer = ampacheDataSource.getDeletedSongs()
        while (listOnServer != null) {
            libraryDatabase.getSongDao().deleteWithId(listOnServer.map { it.toDbSongId() })
            listOnServer = ampacheDataSource.getDeletedSongs()
        }
    }

    /**
     * Private methods: commons
     */

    private suspend fun <SERVER_TYPE> newList(
        getListOnServer: AmpacheDataSource.() -> Flow<List<SERVER_TYPE>>,
        saveToDatabase: suspend (List<SERVER_TYPE>?) -> Unit
    ) {
        ampacheDataSource.getListOnServer()
            .flowOn(Dispatchers.IO)
            .onEach(saveToDatabase)
            .flowOn(Dispatchers.IO)
            .collect()
    }

    private suspend fun <SERVER_TYPE> updateList(
        from: Calendar,
        getListOnServer: AmpacheDataSource.(Calendar) -> Flow<List<SERVER_TYPE>?>,
        saveToDatabase: suspend (List<SERVER_TYPE>?) -> Unit
    ) {
        ampacheDataSource.getListOnServer(from)
            .flowOn(Dispatchers.IO)
            .onEach(saveToDatabase)
            .flowOn(Dispatchers.IO)
            .collect()
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

    companion object {
        const val CHANGE_SONGS = 0
        const val CHANGE_ARTISTS = 1
        const val CHANGE_ALBUMS = 2
        const val CHANGE_GENRES = 3
        const val CHANGE_PLAYLISTS = 4

        private const val LAST_ADD_QUERY = "LAST_ADD_QUERY"
        private const val LAST_UPDATE_QUERY = "LAST_UPDATE_QUERY"
        private const val LAST_CLEAN_QUERY = "LAST_CLEAN_QUERY"

        const val ART_TYPE_SONG = "song"
        const val ART_TYPE_ALBUM = "album"
        const val ART_TYPE_ARTIST = "artist"
        const val ART_TYPE_PLAYLIST = "playlist"
    }
}