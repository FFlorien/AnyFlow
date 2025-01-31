package be.florien.anyflow.feature.sync.service

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateFormat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.logging.eLog
import be.florien.anyflow.common.logging.iLog
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.common.utils.applyPutLong
import be.florien.anyflow.data.server.NetApiError
import be.florien.anyflow.data.server.NetSuccess
import be.florien.anyflow.data.server.datasource.data.AmpacheDataSource
import be.florien.anyflow.data.server.datasource.podcast.AmpachePodcastSource
import be.florien.anyflow.data.server.model.AmpacheAlbum
import be.florien.anyflow.data.server.model.AmpacheApiListResponse
import be.florien.anyflow.data.server.model.AmpacheArtist
import be.florien.anyflow.data.server.model.AmpacheNameId
import be.florien.anyflow.data.server.model.AmpachePlayList
import be.florien.anyflow.data.server.model.AmpachePodcast
import be.florien.anyflow.data.server.model.AmpacheSong
import be.florien.anyflow.data.server.model.AmpacheSongId
import be.florien.anyflow.tags.local.LibraryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val PLAYLIST_DUMP_FOLDER = "playlistDump"
private const val MAX_PLAYLIST_DUMP_FILES = 20

/**
 * Update the local data with the one from the server
 */
@ServerScope
class SyncRepository
@Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val ampacheDataSource: AmpacheDataSource,
    private val ampachePodcastSource: AmpachePodcastSource,
    @Named("preferences") private val sharedPreferences: SharedPreferences,
    private val context: Context
) {
    val libraryPercentageUpdater = MutableLiveData(PercentageUpdate(CHANGE_NONE, 100))
    //region Getter with server updates

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
            //delay(3000)
            playlists()
            podcasts()
            cancelPercentageUpdaters()
        }
    }

    private suspend fun getFromScratch() = withContext(Dispatchers.IO) {
        newGenres()
        newArtists()
        newAlbums()
        newSongs()
        val initialDeletedCount =
            (ampacheDataSource.getDeletedSongs(0, 0) as? NetSuccess)
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
            addGenres(lastUpdate)
            updateGenres(lastUpdate)
            addArtists(lastUpdate)
            updateArtists(lastUpdate)
            addAlbums(lastUpdate)
            updateAlbums(lastUpdate)
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

    //endregion

    //region Private Method : New data

    private suspend fun newGenres() =
        getNewData(
            OFFSET_GENRE,
            CHANGE_GENRES,
            AmpacheDataSource::getNewGenres
        ) { success ->
            libraryDatabase.getGenreDao().upsert(success.data.list.map(AmpacheNameId::toDbGenre))
        }

    private suspend fun newArtists() =
        getNewData(
            OFFSET_ARTIST,
            CHANGE_ARTISTS,
            AmpacheDataSource::getNewArtists
        ) { success ->
            libraryDatabase.getArtistDao().upsert(success.data.list.map(AmpacheArtist::toDbArtist))
        }

    private suspend fun newAlbums() =
        getNewData(
            OFFSET_ALBUM,
            CHANGE_ALBUMS,
            AmpacheDataSource::getNewAlbums
        ) { success ->
            libraryDatabase.getAlbumDao().upsert(success.data.list.map(AmpacheAlbum::toDbAlbum))
        }

    private suspend fun newSongs() =
        getNewData(
            OFFSET_SONG,
            CHANGE_SONGS,
            AmpacheDataSource::getNewSongs
        ) { success ->
            libraryDatabase.getSongDao().upsert(success.data.list.map(AmpacheSong::toDbSong))
            val songGenres = success.data.list.map(AmpacheSong::toDbSongGenres).flatten()
            libraryDatabase.getSongGenreDao().upsert(songGenres)
        }

    private suspend fun playlists() {
        val playlists = ampacheDataSource.getPlaylists()
        val playlistSongs = ampacheDataSource.getPlaylistsWithSongs()
        if (playlists is NetSuccess && playlistSongs is NetSuccess) {
            libraryPercentageUpdater.postValue(PercentageUpdate(CHANGE_PLAYLISTS, -1))
            val beforeUpdateJson = getPlaylistJson()

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

            val postUpdateJson = getPlaylistJson()
            if (beforeUpdateJson != postUpdateJson) {
                val dateFormatted = DateFormat.format("yyyyMMdd-HH:mm:ss", Date())
                val beforeUpdateFileName = "Playlist-$dateFormatted-beforeUpdate.json"
                val postUpdateFileName = "Playlist-$dateFormatted-postUpdate.json"
                writeDbPlaylistToFile(beforeUpdateFileName, beforeUpdateJson)
                writeDbPlaylistToFile(postUpdateFileName, postUpdateJson)
                eLog(PlaylistMismatchException())
                cleanPlaylistFiles()
            }
        }
    }

    private suspend fun getPlaylistJson(): String {
        val playlistSongs = libraryDatabase.getPlaylistSongsDao().getPlaylistWithSongs()
        val content = playlistSongs
            .sortedBy { it.playlist.id }
            .joinToString(separator = ",", prefix = "{", postfix = "}") { entry ->
                val playlistName = entry.playlist.name
                val values = entry.songs
                    .sortedBy { it.id }
                    .joinToString(separator = ",") {
                        "\"${it.id}-${
                            it.title.filter { it.isLetterOrDigit() }.take(20)
                        }\""
                    }
                "\"$playlistName\":[$values]"
            }
        return content
    }

    private fun writeDbPlaylistToFile(fileName: String, content: String) {
        val file = File(context.filesDir, "$PLAYLIST_DUMP_FOLDER/$fileName")
        file.createNewFile()
        val writer = OutputStreamWriter(file.outputStream())
        writer.write(content)
        writer.close()
    }

    private fun cleanPlaylistFiles() {
        val folder = File(context.filesDir, PLAYLIST_DUMP_FOLDER)
        var files = folder.list() ?: return

        while (files.size > MAX_PLAYLIST_DUMP_FILES) {
            val firstFileName = files.minOf { it }
            File(folder, firstFileName).delete()
            files = folder.list() ?: break
        }
    }

    private suspend fun podcasts() {
        val podcasts = ampachePodcastSource.getPodcasts()
        if (podcasts is NetSuccess) {
            libraryPercentageUpdater.postValue(PercentageUpdate(CHANGE_PODCASTS, -1))
            val currentLocalPodcasts = libraryDatabase.getPodcastDao().getPodcastList()
            libraryDatabase.getPodcastEpisodeDao().deleteAllPlaylistSongs()

            val deletedPodcasts = currentLocalPodcasts.filter { localPodcast ->
                podcasts.data.none { localPodcast.id == it.id.toLong() }
            }
            libraryDatabase.getPodcastDao().delete(*deletedPodcasts.toTypedArray())

            val addedPodcasts = podcasts.data.filter { remotePodcast ->
                currentLocalPodcasts.none { remotePodcast.id.toLong() == it.id }
            }
            libraryDatabase.getPodcastDao()
                .upsert(addedPodcasts.map(AmpachePodcast::toDbPodcast))

            val renamedPodcasts = podcasts.data.filter { remotePodcast ->
                val localPodcast =
                    currentLocalPodcasts.firstOrNull { it.id == remotePodcast.id.toLong() }
                localPodcast?.name != remotePodcast.name
            }
            libraryDatabase.getPodcastDao()
                .upsert(renamedPodcasts.map(AmpachePodcast::toDbPodcast))

            val podcastEpisodes =
                podcasts.data.flatMap { podcast ->

                    val episodesResult =
                        ampachePodcastSource.getPodcastEpisodes(podcast.id) as? NetSuccess
                    val episodes = episodesResult?.data

                    if (!episodes.isNullOrEmpty()) {
                        episodes.map { episode ->
                            episode.toDbPodcastEpisode()
                        }
                    } else {
                        emptyList()
                    }
                }
            libraryDatabase.getPodcastEpisodeDao().upsert(podcastEpisodes)
            updatePodcasts(podcasts.data)
        }
    }

    private suspend fun updatePodcasts(podcasts: List<AmpachePodcast>) {
        coroutineScope {
            podcasts.forEach {
                if (it.shouldUpdate()) {
                    launch {
                        try {
                            ampachePodcastSource.updatePodcast(it.id)
                        } catch (exception: Throwable) {
                            //we shouldn't care and continue
                        }
                    }
                }
            }
        }
    }

    private fun AmpachePodcast.shouldUpdate(): Boolean {
        val nowEpoch = Date().time
        return TimeOperations.getDateFromAmpacheComplete(sync_date).timeInMillis.plus(24 * 60 * 60 * 1000L) < nowEpoch
    }

    //endregion

    //region Private Method : added data

    private suspend fun addGenres(from: Calendar) =
        getUpdatedData(
            OFFSET_GENRE,
            CHANGE_GENRES,
            from,
            AmpacheDataSource::getAddedGenres
        ) { success ->
            libraryDatabase.getGenreDao().upsert(success.data.list.map(AmpacheNameId::toDbGenre))
        }

    private suspend fun addArtists(from: Calendar) =
        getUpdatedData(
            OFFSET_ARTIST,
            CHANGE_ARTISTS,
            from,
            AmpacheDataSource::getAddedArtists
        ) { success ->
            libraryDatabase.getArtistDao().upsert(success.data.list.map(AmpacheArtist::toDbArtist))
        }

    private suspend fun addAlbums(from: Calendar) =
        getUpdatedData(
            OFFSET_ALBUM,
            CHANGE_ALBUMS,
            from,
            AmpacheDataSource::getAddedAlbums
        ) { success ->
            libraryDatabase.getAlbumDao().upsert(success.data.list.map(AmpacheAlbum::toDbAlbum))
        }

    private suspend fun addSongs(from: Calendar) =
        getUpdatedData(
            OFFSET_SONG,
            CHANGE_SONGS,
            from,
            AmpacheDataSource::getAddedSongs
        ) { success ->
            libraryDatabase.getSongDao().upsert(success.data.list.map(AmpacheSong::toDbSong))
            val songGenres = success.data.list.map(AmpacheSong::toDbSongGenres).flatten()
            libraryDatabase.getSongGenreDao().upsert(songGenres)
        }

    //endregion

    //region Private Method : Updated data

    private suspend fun updateGenres(from: Calendar) =
        getUpdatedData(
            OFFSET_GENRE,
            CHANGE_GENRES,
            from,
            AmpacheDataSource::getUpdatedGenres
        ) { success ->
            libraryDatabase.getGenreDao().upsert(success.data.list.map(AmpacheNameId::toDbGenre))
        }

    private suspend fun updateArtists(from: Calendar) =
        getUpdatedData(
            OFFSET_ARTIST,
            CHANGE_ARTISTS,
            from,
            AmpacheDataSource::getUpdatedArtists
        ) { success ->
            libraryDatabase.getArtistDao().upsert(success.data.list.map(AmpacheArtist::toDbArtist))
        }

    private suspend fun updateAlbums(from: Calendar) =
        getUpdatedData(
            OFFSET_ALBUM,
            CHANGE_ALBUMS,
            from,
            AmpacheDataSource::getUpdatedAlbums
        ) { success ->
            libraryDatabase.getAlbumDao().upsert(success.data.list.map(AmpacheAlbum::toDbAlbum))
        }

    private suspend fun updateSongs(from: Calendar) =
        getUpdatedData(
            OFFSET_SONG,
            CHANGE_SONGS,
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
            CHANGE_SONGS,
            AmpacheDataSource::getDeletedSongs
        ) { success ->
            libraryDatabase.getSongDao()
                .deleteWithId(success.data.list.map(AmpacheSongId::toDbSongId))
        }
    //endregion

    //region Private method : Utilities
    private suspend fun <V, T : AmpacheApiListResponse<V>> getNewData(
        offsetKey: String,
        percentageUpdater: Int,
        getFromApi: suspend AmpacheDataSource.(Int, Int) -> be.florien.anyflow.data.server.NetResult<T>,
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
        percentageUpdater: Int,
        calendar: Calendar,
        getFromApi: suspend AmpacheDataSource.(Int, Int, Calendar) -> be.florien.anyflow.data.server.NetResult<T>,
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
        percentageUpdaterKey: Int,
        getFromApi: suspend (Int, Int) -> be.florien.anyflow.data.server.NetResult<T>,
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

                is be.florien.anyflow.data.server.NetThrowable -> {
                    eLog(result.throwable, "Encountered exception during syncing for $offsetKey")
                    break
                }
            }
            val percentage = if (count == 0) 100 else (offset * 100) / count
            libraryPercentageUpdater.postValue(PercentageUpdate(percentageUpdaterKey, percentage))
            result = getFromApi(offset, limit)
        }
    }

    private fun cancelPercentageUpdaters() {
        libraryPercentageUpdater.postValue(PercentageUpdate(CHANGE_NONE, 0))
    }
    //endregion

    data class PercentageUpdate(val subject: Int, val percent: Int)

    companion object {
        const val CHANGE_NONE = -1
        const val CHANGE_SONGS = 0
        const val CHANGE_ARTISTS = 1
        const val CHANGE_ALBUMS = 2
        const val CHANGE_GENRES = 3
        const val CHANGE_PLAYLISTS = 4
        const val CHANGE_PODCASTS = 5

        private const val ITEM_LIMIT: Int = 250

        private const val LAST_UPDATE_QUERY = "LAST_UPDATE_QUERY"

        private const val OFFSET_SONG = "OFFSET_SONG"
        private const val OFFSET_GENRE = "OFFSET_GENRE"
        private const val OFFSET_ARTIST = "OFFSET_ARTIST"
        private const val OFFSET_ALBUM = "OFFSET_ALBUM"

        //Don't reset this value, deleted doesn't have a "from" parameter
        private const val OFFSET_DELETED = "OFFSET_DELETED"

    }
}