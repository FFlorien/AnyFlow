package be.florien.anyflow.feature.sync

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.utils.TimeOperations
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.server.datasource.data.AmpacheDataSource
import be.florien.anyflow.data.server.datasource.podcast.AmpachePodcastSource
import be.florien.anyflow.data.server.NetApiError
import be.florien.anyflow.data.server.NetResult
import be.florien.anyflow.data.server.NetSuccess
import be.florien.anyflow.data.server.NetThrowable
import be.florien.anyflow.data.server.model.AmpacheAlbum
import be.florien.anyflow.data.server.model.AmpacheApiListResponse
import be.florien.anyflow.data.server.model.AmpacheArtist
import be.florien.anyflow.data.server.model.AmpacheNameId
import be.florien.anyflow.data.server.model.AmpachePlayList
import be.florien.anyflow.data.server.model.AmpachePodcast
import be.florien.anyflow.data.server.model.AmpacheSong
import be.florien.anyflow.data.server.model.AmpacheSongId
import be.florien.anyflow.data.toDbAlbum
import be.florien.anyflow.data.toDbArtist
import be.florien.anyflow.data.toDbGenre
import be.florien.anyflow.data.toDbPlaylist
import be.florien.anyflow.data.toDbPlaylistSongs
import be.florien.anyflow.data.toDbPodcast
import be.florien.anyflow.data.toDbPodcastEpisode
import be.florien.anyflow.data.toDbSong
import be.florien.anyflow.data.toDbSongGenres
import be.florien.anyflow.data.toDbSongId
import be.florien.anyflow.utils.applyPutLong
import be.florien.anyflow.logging.eLog
import be.florien.anyflow.logging.iLog
import be.florien.anyflow.data.server.di.ServerScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

/**
 * Update the local data with the one from the server
 */
@ServerScope
class SyncRepository
@Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val ampacheDataSource: AmpacheDataSource,
    private val ampachePodcastSource: AmpachePodcastSource,
    @Named("preferences") private val sharedPreferences: SharedPreferences
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
        withContext(Dispatchers.IO) {
            if (libraryDatabase.getSongDao().songCount() == 0) {
                //todo come on, there has to be a better way to do this check!
                getFromScratch()
            } else {
                iLog("update")
                update()
            }
            playlists()
            podcasts()
            cancelPercentageUpdaters()
        }
    }

    private suspend fun getFromScratch() = withContext(Dispatchers.IO) {
        notifyUpdate(CHANGE_GENRES)
        newGenres()
        notifyUpdate(CHANGE_GENRES)
        newArtists()
        notifyUpdate(CHANGE_GENRES)
        newAlbums()
        notifyUpdate(CHANGE_GENRES)
        newSongs()
        val initialDeletedCount = (ampacheDataSource.getDeletedSongs(0, 0) as? NetSuccess)
            ?.data
            ?.total_count
            ?: 0
        val currentMillis = TimeOperations.getCurrentDate().timeInMillis
        sharedPreferences.edit().apply {
            putLong(LAST_UPDATE_QUERY, currentMillis)
            putInt(OFFSET_DELETED, initialDeletedCount)
        }.apply()
    }

    private suspend fun update() =
        sync { lastUpdate ->
            notifyUpdate(CHANGE_GENRES)
            addGenres(lastUpdate)
            updateGenres(lastUpdate)
            notifyUpdate(CHANGE_ARTISTS)
            addArtists(lastUpdate)
            updateArtists(lastUpdate)
            notifyUpdate(CHANGE_ALBUMS)
            addAlbums(lastUpdate)
            updateAlbums(lastUpdate)
            notifyUpdate(CHANGE_SONGS)
            addSongs(lastUpdate)
            updateSongs(lastUpdate)
            updateDeletedSongs() //todo remove unused artists/albums/genres
        }

    private suspend fun sync(sync: suspend (Calendar) -> Unit) =
        withContext(Dispatchers.IO) {
            val nowDate = TimeOperations.getCurrentDate()
            val lastUpdateMillis = sharedPreferences.getLong(LAST_UPDATE_QUERY, 0L)
            val lastUpdate = TimeOperations.getDateFromMillis(lastUpdateMillis)
            sync(lastUpdate)
            sharedPreferences.applyPutLong(LAST_UPDATE_QUERY, nowDate.timeInMillis)
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
            libraryDatabase.getGenreDao().upsert(success.data.list.map(AmpacheNameId::toDbGenre))
        }

    private suspend fun newArtists() =
        getNewData(
            OFFSET_ARTIST,
            artistsPercentageUpdater,
            AmpacheDataSource::getNewArtists
        ) { success ->
            libraryDatabase.getArtistDao().upsert(success.data.list.map(AmpacheArtist::toDbArtist))
        }

    private suspend fun newAlbums() =
        getNewData(
            OFFSET_ALBUM,
            albumsPercentageUpdater,
            AmpacheDataSource::getNewAlbums
        ) { success ->
            libraryDatabase.getAlbumDao().upsert(success.data.list.map(AmpacheAlbum::toDbAlbum))
        }

    private suspend fun newSongs() =
        getNewData(
            OFFSET_SONG,
            songsPercentageUpdater,
            AmpacheDataSource::getNewSongs
        ) { success ->
            libraryDatabase.getSongDao().upsert(success.data.list.map(AmpacheSong::toDbSong))
            val songGenres = success.data.list.map(AmpacheSong::toDbSongGenres).flatten()
            libraryDatabase.getSongGenreDao().upsert(songGenres)
        }

    suspend fun playlists() {
        notifyUpdate(CHANGE_PLAYLISTS)
        val playlists = ampacheDataSource.getPlaylists()
        val playlistSongs = ampacheDataSource.getPlaylistsWithSongs()
        if (playlists is NetSuccess && playlistSongs is NetSuccess) {
            val currentLocalPlaylists = libraryDatabase.getPlaylistDao().getPlaylistsList()
            libraryDatabase.getPlaylistSongsDao().deleteAllPlaylistSongs()

            val deletedPlaylists = currentLocalPlaylists.filter { localPlaylist ->
                playlists.data.list.none { localPlaylist.id == it.id }
            }
            libraryDatabase.getPlaylistDao().delete(*deletedPlaylists.toTypedArray())

            val addedPlaylists = playlists.data.list.filter { remotePlaylist ->
                currentLocalPlaylists.none { remotePlaylist.id == it.id }
            }
            libraryDatabase.getPlaylistDao()
                .upsert(addedPlaylists.map(AmpachePlayList::toDbPlaylist))

            val renamedPlaylists = playlists.data.list.filter { remotePlaylist ->
                val localPlaylist = currentLocalPlaylists.firstOrNull { it.id == remotePlaylist.id }
                localPlaylist?.name != remotePlaylist.name
            }
            libraryDatabase.getPlaylistDao()
                .upsert(renamedPlaylists.map(AmpachePlayList::toDbPlaylist))

            val playlistSongsDb = playlistSongs.data.playlistList.toDbPlaylistSongs()
            libraryDatabase.getPlaylistSongsDao().upsert(playlistSongsDb)
        }
    }

    private suspend fun podcasts() {
        notifyUpdate(CHANGE_PLAYLISTS)
        val podcasts = ampachePodcastSource.getPodcasts()
        if (podcasts is NetSuccess) {
            podcasts.data.forEach {
                ampachePodcastSource.updatePodcast(it.id)
            }
            val podcastsWithEpisodes = ampachePodcastSource.getPodcastsWithEpisodes()
            if (podcastsWithEpisodes is NetSuccess) {
                val currentLocalPodcasts = libraryDatabase.getPodcastDao().getPodcastList()
                libraryDatabase.getPodcastEpisodeDao().deleteAllPlaylistSongs()

                val deletedPodcasts = currentLocalPodcasts.filter { localPodcast ->
                    podcastsWithEpisodes.data.none { localPodcast.id == it.id.toLong() }
                }
                libraryDatabase.getPodcastDao().delete(*deletedPodcasts.toTypedArray())

                val addedPodcasts = podcastsWithEpisodes.data.filter { remotePodcast ->
                    currentLocalPodcasts.none { remotePodcast.id.toLong() == it.id }
                }
                libraryDatabase.getPodcastDao()
                    .upsert(addedPodcasts.map(AmpachePodcast::toDbPodcast))

                val renamedPodcasts = podcastsWithEpisodes.data.filter { remotePodcast ->
                    val localPodcast =
                        currentLocalPodcasts.firstOrNull { it.id == remotePodcast.id.toLong() }
                    localPodcast?.name != remotePodcast.name
                }
                libraryDatabase.getPodcastDao()
                    .upsert(renamedPodcasts.map(AmpachePodcast::toDbPodcast))

                val podcastEpisodes =
                    podcastsWithEpisodes.data.flatMap { podcast ->
                        podcast.podcast_episode.map { episode ->
                            episode.toDbPodcastEpisode()
                        }
                    }
                libraryDatabase.getPodcastEpisodeDao().upsert(podcastEpisodes)
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
            libraryDatabase.getGenreDao().upsert(success.data.list.map(AmpacheNameId::toDbGenre))
        }

    private suspend fun addArtists(from: Calendar) =
        getUpdatedData(
            OFFSET_ARTIST,
            artistsPercentageUpdater,
            from,
            AmpacheDataSource::getAddedArtists
        ) { success ->
            libraryDatabase.getArtistDao().upsert(success.data.list.map(AmpacheArtist::toDbArtist))
        }

    private suspend fun addAlbums(from: Calendar) =
        getUpdatedData(
            OFFSET_ALBUM,
            albumsPercentageUpdater,
            from,
            AmpacheDataSource::getAddedAlbums
        ) { success ->
            libraryDatabase.getAlbumDao().upsert(success.data.list.map(AmpacheAlbum::toDbAlbum))
        }

    private suspend fun addSongs(from: Calendar) =
        getUpdatedData(
            OFFSET_SONG,
            songsPercentageUpdater,
            from,
            AmpacheDataSource::getAddedSongs
        ) { success ->
            libraryDatabase.getSongDao().upsert(success.data.list.map(AmpacheSong::toDbSong))
            val songGenres = success.data.list.map(AmpacheSong::toDbSongGenres).flatten()
            libraryDatabase.getSongGenreDao().upsert(songGenres)
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
            libraryDatabase.getGenreDao().upsert(success.data.list.map(AmpacheNameId::toDbGenre))
        }

    private suspend fun updateArtists(from: Calendar) =
        getUpdatedData(
            OFFSET_ARTIST,
            artistsPercentageUpdater,
            from,
            AmpacheDataSource::getUpdatedArtists
        ) { success ->
            libraryDatabase.getArtistDao().upsert(success.data.list.map(AmpacheArtist::toDbArtist))
        }

    private suspend fun updateAlbums(from: Calendar) =
        getUpdatedData(
            OFFSET_ALBUM,
            albumsPercentageUpdater,
            from,
            AmpacheDataSource::getUpdatedAlbums
        ) { success ->
            libraryDatabase.getAlbumDao().upsert(success.data.list.map(AmpacheAlbum::toDbAlbum))
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
            libraryDatabase.getSongDao().upsert(songs)
            val songGenres = success.data.list.map(AmpacheSong::toDbSongGenres).flatten()
            libraryDatabase.getSongGenreDao().upsert(songGenres)
        }

    private suspend fun updateDeletedSongs() =
        getNewData(
            OFFSET_DELETED,
            songsPercentageUpdater,
            AmpacheDataSource::getDeletedSongs
        ) { success ->
            libraryDatabase.getSongDao()
                .deleteWithId(success.data.list.map(AmpacheSongId::toDbSongId))
        }

    private suspend fun <V, T : AmpacheApiListResponse<V>> getNewData(
        offsetKey: String,
        percentageUpdater: MutableLiveData<Int>,
        getFromApi: suspend AmpacheDataSource.(Int, Int) -> NetResult<T>,
        updateDb: suspend (NetSuccess<T>) -> Unit
    ) {
        getData(
            offsetKey,
            percentageUpdater,
            { offset, limit ->
                ampacheDataSource.getFromApi(offset, limit)
            },
            { netResult, newOffset ->
                updateDb(netResult)
                sharedPreferences.edit().putInt(offsetKey, newOffset).apply()
            }
        )
    }

    private suspend fun <V, T : AmpacheApiListResponse<V>> getUpdatedData(
        offsetKey: String,
        percentageUpdater: MutableLiveData<Int>,
        calendar: Calendar,
        getFromApi: suspend AmpacheDataSource.(Int, Int, Calendar) -> NetResult<T>,
        updateDb: suspend (NetSuccess<T>) -> Unit
    ) {
        getData(
            offsetKey,
            percentageUpdater,
            { offset, limit ->
                ampacheDataSource.getFromApi(offset, limit, calendar)
            },
            { netResult, newOffset ->
                updateDb(netResult)
                sharedPreferences.edit().putInt(offsetKey, newOffset).apply()
            }
        )
    }

    private suspend fun <V, T : AmpacheApiListResponse<V>> getData(
        offsetKey: String,
        percentageUpdater: MutableLiveData<Int>,
        getFromApi: suspend (Int, Int) -> NetResult<T>,
        updateLocalData: suspend (NetSuccess<T>, Int) -> Unit
    ) {
        var offset = sharedPreferences.getInt(offsetKey, 0)
        var count = Int.MAX_VALUE
        var limit = ITEM_LIMIT
        var result = getFromApi(offset, limit)
        while (offset < count) {
            when (result) {
                is NetSuccess -> {
                    count = result.data.total_count
                    offset += result.data.list.size
                    updateLocalData(result, offset)
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
            val percentage = if (count == 0) 100 else (offset * 100) / count
            percentageUpdater.postValue(percentage)
            result = getFromApi(offset, limit)
        }
    }

    private fun notifyUpdate(changeSubject: Int) {
        (changeUpdater as MutableLiveData).postValue(changeSubject)
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

        private const val LAST_UPDATE_QUERY = "LAST_UPDATE_QUERY"

        const val ART_TYPE_SONG = "song"
        const val ART_TYPE_ALBUM = "album"
        const val ART_TYPE_ARTIST = "artist"
        const val ART_TYPE_PLAYLIST = "playlist"
        const val ART_TYPE_PODCAST = "podcast"

        private const val OFFSET_SONG = "OFFSET_SONG"
        private const val OFFSET_GENRE = "OFFSET_GENRE"
        private const val OFFSET_ARTIST = "OFFSET_ARTIST"
        private const val OFFSET_ALBUM = "OFFSET_ALBUM"

        //Don't reset this value, deleted doesn't have a "from" parameter
        private const val OFFSET_DELETED = "OFFSET_DELETED"
    }
}