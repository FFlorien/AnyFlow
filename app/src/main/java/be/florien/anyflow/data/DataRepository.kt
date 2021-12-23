package be.florien.anyflow.data

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.*
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.view.*
import be.florien.anyflow.extension.applyPutLong
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Update the local data with the one from the server
 */
@Singleton
class DataRepository
@Inject constructor(
        private val libraryDatabase: LibraryDatabase,
        private val ampacheConnection: AmpacheConnection,
        private val sharedPreferences: SharedPreferences,
        private val downloadManager: DownloadManager
) {

    private fun lastAcceptableUpdate() = TimeOperations.getCurrentDatePlus(Calendar.HOUR, -1)

    /**
     * Getter with server updates
     */

    suspend fun updateAll() = withContext(Dispatchers.IO) {
        updateArtistsAsync()
        updateAlbumsAsync()
        updateSongsAsync()
        updatePlaylistAsync()
    }

    suspend fun getSongAtPosition(position: Int) =
            withContext(Dispatchers.IO) { libraryDatabase.getSongAtPosition(position)?.toViewSongInfo() }

    suspend fun getPositionForSong(songId: Long) =
            withContext(Dispatchers.IO) { libraryDatabase.getPositionForSong(songId) }

    suspend fun getSongById(songId: Long) =
            withContext(Dispatchers.IO) { libraryDatabase.getSongById(songId)?.toViewSongInfo() }

    fun getSongsInQueueOrder() =
            convertToPagingLiveData(libraryDatabase.getSongsInQueueOrder().map { it.toViewSong() })

    fun getIdsInQueueOrder() = libraryDatabase.getIdsInQueueOrder()

    fun searchSongs(filter: String) = libraryDatabase.searchSongs("%$filter%")

    suspend fun getQueueSize(): Int? = withContext(Dispatchers.IO) { libraryDatabase.getQueueSize() }

    fun <T : Any> getAlbums(mapping: (DbAlbumDisplay) -> T): LiveData<PagingData<T>> =
            convertToPagingLiveData(libraryDatabase.getAlbums().map { mapping(it) })

    fun <T : Any> getArtists(mapping: (DbArtistDisplay) -> T): LiveData<PagingData<T>> =
            convertToPagingLiveData(libraryDatabase.getAlbumArtists().map { mapping(it) })

    fun <T : Any> getGenres(mapping: (String) -> T): LiveData<PagingData<T>> =
            convertToPagingLiveData(libraryDatabase.getGenres().map { mapping(it) })

    fun <T : Any> getSongs(mapping: (DbSongDisplay) -> T): LiveData<PagingData<T>> =
            convertToPagingLiveData(libraryDatabase.getSongsInAlphabeticalOrder().map { mapping(it) })

    fun <T : Any> getPlaylists(mapping: (DbPlaylist) -> T): LiveData<PagingData<T>> =
            convertToPagingLiveData(libraryDatabase.getPlaylists().map { mapping(it) })

    /**
     * Filtered lists
     */

    fun <T : Any> getAlbumsFiltered(
            filter: String,
            mapping: (DbAlbumDisplay) -> T
    ): LiveData<PagingData<T>> =
            convertToPagingLiveData(libraryDatabase.getAlbumsFiltered("%$filter%").map { mapping(it) })

    suspend fun <T : Any> getAlbumsFilteredList(
            filter: String,
            mapping: (DbAlbumDisplay) -> T
    ): List<T> =
            libraryDatabase.getAlbumsFilteredList("%$filter%").map { item -> (mapping(item)) }


    fun <T : Any> getArtistsFiltered(
            filter: String,
            mapping: (DbArtistDisplay) -> T
    ): LiveData<PagingData<T>> =
            convertToPagingLiveData(libraryDatabase.getAlbumArtistsFiltered("%$filter%").map { mapping(it) })

    suspend fun <T : Any> getArtistsFilteredList(
            filter: String,
            mapping: (DbArtistDisplay) -> T
    ): List<T> =
            libraryDatabase.getAlbumArtistsFilteredList("%$filter%").map { item -> (mapping(item)) }


    fun <T : Any> getGenresFiltered(
            filter: String,
            mapping: (String) -> T
    ): LiveData<PagingData<T>> =
            convertToPagingLiveData(libraryDatabase.getGenresFiltered("%$filter%").map { mapping(it) })

    suspend fun <T : Any> getGenresFilteredList(
            filter: String,
            mapping: (String) -> T
    ): List<T> =
            libraryDatabase.getGenresFilteredList("%$filter%").map { item -> (mapping(item)) }

    fun <T : Any> getSongsFiltered(
            filter: String,
            mapping: (DbSongDisplay) -> T
    ): LiveData<PagingData<T>> =
            convertToPagingLiveData(libraryDatabase.getSongsFiltered("%$filter%").map { mapping(it) })

    suspend fun <T : Any> getSongsFilteredList(
            filter: String,
            mapping: (DbSongDisplay) -> T
    ): List<T> =
            libraryDatabase.getSongsFilteredList("%$filter%").map { item -> (mapping(item)) }

    fun <T : Any> getPlaylistsFiltered(
            filter: String,
            mapping: (DbPlaylist) -> T
    ): LiveData<PagingData<T>> =
            convertToPagingLiveData(libraryDatabase.getPlaylistsFiltered("%$filter%").map { mapping(it) })

    suspend fun <T : Any> getPlaylistsFilteredList(
            filter: String,
            mapping: (DbPlaylist) -> T
    ): List<T> =
            libraryDatabase.getPlaylistsFilteredList("%$filter%").map { item -> (mapping(item)) }

    /**
     * Playlists modification
     */

    suspend fun addSongToPlaylist(songId: Long, playlistId: Long) {
        ampacheConnection.addSongToPlaylist(songId, playlistId)
        libraryDatabase.addPlaylistSongs(listOf(DbPlaylistSongs(songId, playlistId)))
    }

    suspend fun createPlaylist(name: String) {
        ampacheConnection.createPlaylist(name)
        updatePlaylistAsync()
    }

    /**
     * Orders
     */

    fun getOrders() =
            libraryDatabase.getOrders().map { list -> list.map { item -> item.toViewOrder() } }

    suspend fun getOrderlessQueue(filterList: List<Filter<*>>, orderList: List<Order>): List<Long> =
            withContext(Dispatchers.IO) {
                libraryDatabase.getSongsFromQuery(
                        getQueryForSongs(filterList.map { it.toDbFilter(DbFilterGroup.CURRENT_FILTER_GROUP_ID) }, orderList))
            }

    suspend fun setOrders(orders: List<Order>) =
            withContext(Dispatchers.IO) {
                libraryDatabase.setOrders(orders.map { it.toDbOrder() })
            }

    suspend fun saveQueueOrder(listToSave: MutableList<Long>) {
        libraryDatabase.saveQueueOrder(listToSave)
    }

    /**
     * Alarms
     */

    suspend fun addAlarm(alarm: Alarm) = libraryDatabase.addAlarm(alarm.toDbAlarm())

    fun getAlarms(): LiveData<List<Alarm>> = libraryDatabase.getAlarms().map { list -> list.map { it.toViewAlarm() } }
    suspend fun getAlarmList(): List<Alarm> = libraryDatabase.getAlarmList().map { it.toViewAlarm() }

    suspend fun activateAlarm(alarm: Alarm) {
        val newAlarm = alarm.copy(active = true)
        libraryDatabase.updateAlarm(newAlarm.toDbAlarm())
    }

    suspend fun deactivateAlarm(alarm: Alarm) {
        val newAlarm = alarm.copy(active = false)
        libraryDatabase.updateAlarm(newAlarm.toDbAlarm())
    }

    suspend fun editAlarm(alarm: Alarm) {
        libraryDatabase.updateAlarm(alarm.toDbAlarm())
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        libraryDatabase.deleteAlarm(alarm.toDbAlarm())
    }

    /**
     * Filter groups
     */

    suspend fun createFilterGroup(filterList: List<Filter<*>>, name: String) =
            withContext(Dispatchers.IO) {
                libraryDatabase.createFilterGroup(filterList.map {
                    it.toDbFilter(-1)
                }, name)
            }

    fun getFilterGroups() = libraryDatabase.getFilterGroups()
            .map { groupList -> groupList.map { it.toViewFilterGroup() } }

    suspend fun setSavedGroupAsCurrentFilters(filterGroup: FilterGroup) =
            withContext(Dispatchers.IO) { libraryDatabase.setSavedGroupAsCurrentFilters(filterGroup.toDbFilterGroup()) }

    fun getCurrentFilters(): LiveData<List<Filter<*>>> = libraryDatabase.getCurrentFilters()
            .map { filterList -> filterList.map { it.toViewFilter() } }

    suspend fun setCurrentFilters(filterList: List<Filter<*>>) = withContext(Dispatchers.IO) {
        libraryDatabase.setCurrentFilters(filterList.map {
            it.toDbFilter(1)
        })
    }

    /**
     * Download status
     */

    fun hasDownloaded(song: SongInfo): Boolean = downloadManager.downloadIndex.getDownload(song.url) != null


    /**
     * Private Method
     */

    private suspend fun updateSongsAsync() = updateListAsync(
            LAST_SONG_UPDATE,
            AmpacheConnection::getSongs,
    ) { ampacheSongList ->
        if (ampacheSongList != null) {
            val songs = ampacheSongList.map { it.toDbSong() }
            libraryDatabase.addSongs(songs)
            libraryDatabase.correctAlbumArtist(songs)
        }
    }

    private suspend fun updateArtistsAsync() = updateListAsync(
            LAST_ARTIST_UPDATE,
            AmpacheConnection::getArtists,
    ) { ampacheArtistList ->
        if (ampacheArtistList != null) {
            libraryDatabase.addArtists(ampacheArtistList.map { it.toDbArtist() })
        }
    }

    private suspend fun updateAlbumsAsync() = updateListAsync(
            LAST_ALBUM_UPDATE,
            AmpacheConnection::getAlbums,
    ) { ampacheAlbumList ->
        if (ampacheAlbumList != null) {
            libraryDatabase.addAlbums(ampacheAlbumList.map { it.toDbAlbum() })
        }
    }

    private suspend fun <SERVER_TYPE> updateListAsync(
            updatePreferenceName: String,
            getListOnServer: suspend AmpacheConnection.(Calendar) -> List<SERVER_TYPE>?,
            saveToDatabase: suspend (List<SERVER_TYPE>?) -> Unit
    ) {

        val nowDate = TimeOperations.getCurrentDate()
        val lastUpdate = TimeOperations.getDateFromMillis(sharedPreferences.getLong(updatePreferenceName, 0))
        val lastAcceptableUpdate = lastAcceptableUpdate()

        if (lastUpdate.before(lastAcceptableUpdate)) {
            var listOnServer = ampacheConnection.getListOnServer(lastUpdate)
            while (listOnServer != null) {
//                listOnServer = if (listOnServer.getError().code == 401) {
//                    songServerConnection.reconnect { songServerConnection.getListOnServer(lastUpdate) }
//                } else {
//                    listOnServer
//                }
                saveToDatabase(listOnServer)
                listOnServer = ampacheConnection.getListOnServer(lastUpdate)
            }
            sharedPreferences.applyPutLong(updatePreferenceName, nowDate.timeInMillis)
        }
    }

    private suspend fun updatePlaylistAsync() {
        var listOnServer = ampacheConnection.getPlaylists()
        while (listOnServer != null) {
            libraryDatabase.addPlayLists(listOnServer.map { it.toDbPlaylist() })
            for (playlist in listOnServer) {
                updatePlaylistSongListAsync(playlist.id)
            }
            listOnServer = ampacheConnection.getPlaylists()
        }
    }

    private suspend fun updatePlaylistSongListAsync(playlistId: Long) {
        var listOnServer = ampacheConnection.getPlaylistsSongs(playlistId)
        while (listOnServer != null) {
            libraryDatabase.addPlaylistSongs(listOnServer.map { DbPlaylistSongs(it.id, playlistId) })
            listOnServer = ampacheConnection.getPlaylistsSongs(playlistId)
        }
    }

    private fun <T : Any> convertToPagingLiveData(dataSourceFactory: DataSource.Factory<Int, T>): LiveData<PagingData<T>> =
            Pager(PagingConfig(100), 0, dataSourceFactory.asPagingSourceFactory(Dispatchers.IO)).liveData

    private fun getQueryForSongs(dbFilters: List<DbFilter>, orderList: List<Order>): String {

        fun constructOrderStatement(): String {
            val filteredOrderedList =
                    orderList.filter { it.orderingType != Order.Ordering.PRECISE_POSITION }

            val isSorted =
                    filteredOrderedList.isNotEmpty() && filteredOrderedList.all { it.orderingType != Order.Ordering.RANDOM }

            var orderStatement = if (isSorted) {
                " ORDER BY"
            } else {
                ""
            }

            if (isSorted) {
                filteredOrderedList.forEachIndexed { index, order ->
                    orderStatement += when (order.orderingSubject) {
                        Order.Subject.ALL -> " song.id"
                        Order.Subject.ARTIST -> " song.artistName"
                        Order.Subject.ALBUM_ARTIST -> " song.albumArtistName"
                        Order.Subject.ALBUM -> " song.albumName"
                        Order.Subject.ALBUM_ID -> " song.albumId"
                        Order.Subject.YEAR -> " song.year"
                        Order.Subject.GENRE -> " song.genre"
                        Order.Subject.TRACK -> " song.track"
                        Order.Subject.TITLE -> " song.title"
                    }
                    orderStatement += when (order.orderingType) {
                        Order.Ordering.ASCENDING -> " ASC"
                        Order.Ordering.DESCENDING -> " DESC"
                        else -> ""
                    }
                    if (index < filteredOrderedList.size - 1 && orderStatement.last() != ',') {
                        orderStatement += ","
                    }
                }
            }

            return orderStatement
        }

        return "SELECT id FROM song" + constructJoinStatement(dbFilters) + constructWhereStatement(dbFilters) + constructOrderStatement()
    }

    private fun constructJoinStatement(filterList: List<DbFilter>): String {
        return filterList.filter { !it.joinClause.isNullOrBlank() }.joinToString(separator = " ", prefix = " ", postfix = " ") {
            it.joinClause!!
        }
    }

    private fun constructWhereStatement(filterList: List<DbFilter>): String {
        var whereStatement = if (filterList.isNotEmpty()) {
            " WHERE"
        } else {
            ""
        }

        filterList.forEachIndexed { index, filter ->
            whereStatement += when (filter.clause) {
                DbFilter.SONG_ID,
                DbFilter.ARTIST_ID,
                DbFilter.ALBUM_ARTIST_ID,
                DbFilter.PLAYLIST_ID,
                DbFilter.ALBUM_ID -> " ${filter.clause} ${filter.argument}"
                DbFilter.DOWNLOAD_IN -> {
                    val downloadedCursor = downloadManager.downloadIndex.getDownloads(Download.STATE_COMPLETED)
                    downloadedCursor.moveToFirst()
                    val downloadedList = mutableListOf<Download>()
                    while (!downloadedCursor.isAfterLast) {
                        downloadedList.add(downloadedCursor.download)
                        downloadedCursor.moveToNext()
                    }
                    downloadedCursor.close()

                    val downloadedUrls = downloadedList.map { it.request.id }.joinToString { "\'$it\'" }
                    " ${filter.clause} ($downloadedUrls)"
                }
                else -> " ${filter.clause} \"${filter.argument}\""
            }
            if (index < filterList.size - 1) {
                whereStatement += " OR"
            }
        }

        return whereStatement
    }

    suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Boolean = libraryDatabase.isPlaylistContainingSong(playlistId, songId)

    companion object {
        private const val LAST_SONG_UPDATE = "LAST_SONG_UPDATE"

        // updated because the art was added , see LibraryDatabase migration 1->2
        private const val LAST_ARTIST_UPDATE = "LAST_ARTIST_UPDATE_v1"
        private const val LAST_ALBUM_UPDATE = "LAST_ALBUM_UPDATE"
        private const val LAST_PLAYLIST_UPDATE = "LAST_PLAYLIST_UPDATE"
        private const val LAST_PLAYLIST_SONGS_UPDATE = "LAST_PLAYLIST_SONGS_UPDATE"
    }
}