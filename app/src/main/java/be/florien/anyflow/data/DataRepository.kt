package be.florien.anyflow.data

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.*
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.view.*
import be.florien.anyflow.extension.applyPutLong
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
    private val ampacheDataSource: AmpacheDataSource,
    private val sharedPreferences: SharedPreferences,
    private val downloadManager: DownloadManager
) {

    /**
     * Getter with server updates
     */

    suspend fun syncAll() {
        if (libraryDatabase.getSongCount() == 0) {
            getFromScratch()
        } else {
            addAll()
            updateAll()
            cleanAll()
        }
    }

    private suspend fun getFromScratch() = withContext(Dispatchers.IO) {
        newGenres()
        newArtists()
        newAlbums()
        newSongs()
        newPlaylists()
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
            addPlaylists(lastSync)
            ampacheDataSource.resetAddOffsets()
        }

    private suspend fun updateAll() =
        syncIfOutdated(AmpacheDataSource.SERVER_UPDATE, LAST_UPDATE_QUERY) { lastSync ->
            updateGenres(lastSync)
            updateArtists(lastSync)
            updateAlbums(lastSync)
            updateSongs(lastSync)
            updatePlaylists(lastSync)
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
     * Songs related methods
     */

    suspend fun getSongAtPosition(position: Int) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getSongAtPosition(position)?.toViewSongInfo()
        }

    suspend fun getPositionForSong(songId: Long) =
        withContext(Dispatchers.IO) { libraryDatabase.getPositionForSong(songId) }

    fun getSongsInQueueOrder() =
        convertToPagingLiveData(libraryDatabase.getSongsInQueueOrder().map { it.toViewSongInfo() })

    fun getIdsInQueueOrder() = libraryDatabase.getIdsInQueueOrder()

    fun searchSongs(filter: String) = libraryDatabase.searchSongs("%$filter%")

    suspend fun updateSongLocalUri(songId: Long, uri: String) {
        libraryDatabase.updateSongLocalUri(songId, uri)
    }

    /**
     * Lists
     */

    suspend fun getQueueSize(): Int? =
        withContext(Dispatchers.IO) { libraryDatabase.getQueueSize() }

    fun <T : Any> getAlbums(mapping: (DbAlbumDisplay) -> T): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getAlbums().map { mapping(it) })

    fun <T : Any> getAlbumArtists(mapping: (DbArtist) -> T): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getAlbumArtists().map { mapping(it) })

    fun <T : Any> getGenres(mapping: (DbGenre) -> T): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getGenres().map { mapping(it) })

    fun <T : Any> getSongs(mapping: (DbSongDisplay) -> T): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getSongsInAlphabeticalOrder().map { mapping(it) })

    fun <T : Any> getPlaylists(mapping: (DbPlaylistWithCount) -> T): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getPlaylists().map { mapping(it) })

    fun getPlaylists(): LiveData<PagingData<Playlist>> =
        convertToPagingLiveData(libraryDatabase.getPlaylists().map { it.toViewPlaylist() })

    fun <T : Any> getPlaylistSongs(
        playlistId: Long,
        mapping: (DbSongDisplay) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getPlaylistSongs(playlistId).map { mapping(it) })

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


    fun <T : Any> getAlbumArtistsFiltered(
        filter: String,
        mapping: (DbArtist) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getAlbumArtistsFiltered("%$filter%").map { mapping(it) })

    suspend fun <T : Any> getAlbumArtistsFilteredList(
        filter: String,
        mapping: (DbArtist) -> T
    ): List<T> =
        libraryDatabase.getAlbumArtistsFilteredList("%$filter%").map { item -> (mapping(item)) }


    fun <T : Any> getGenresFiltered(
        filter: String,
        mapping: (DbGenre) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getGenresFiltered("%$filter%").map { mapping(it) })

    suspend fun <T : Any> getGenresFilteredList(
        filter: String,
        mapping: (DbGenre) -> T
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
        mapping: (DbPlaylistWithCount) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getPlaylistsFiltered("%$filter%").map { mapping(it) })

    suspend fun <T : Any> getPlaylistsFilteredList(
        filter: String,
        mapping: (DbPlaylistWithCount) -> T
    ): List<T> =
        libraryDatabase.getPlaylistsFilteredList("%$filter%").map { item -> (mapping(item)) }

    /**
     * Playlists modification
     */

    suspend fun createPlaylist(name: String) {
        ampacheDataSource.createPlaylist(name)
        addPlaylists(TimeOperations.getCurrentDatePlus(Calendar.HOUR, -1))
    }

    suspend fun deletePlaylist(id: Long) {
        ampacheDataSource.deletePlaylist(id)
        libraryDatabase.deletePlaylist(id)
    }

    suspend fun addSongToPlaylist(songId: Long, playlistId: Long) {
        ampacheDataSource.addSongToPlaylist(songId, playlistId)
        val playlistLastOrder = libraryDatabase.getPlaylistLastOrder(playlistId)
        libraryDatabase.addOrUpdatePlaylistSongs(
            listOf(
                DbPlaylistSongs(
                    playlistLastOrder + 1,
                    songId,
                    playlistId
                )
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        ampacheDataSource.removeSongFromPlaylist(playlistId, songId)
        libraryDatabase.removeSongFromPlaylist(playlistId, songId)
    }

    /**
     * Orders
     */

    fun getOrders() =
        libraryDatabase.getOrders().map { list -> list.map { item -> item.toViewOrder() } }

    suspend fun getOrderlessQueue(filterList: List<Filter<*>>, orderList: List<Order>): List<Long> =
        withContext(Dispatchers.IO) {
            libraryDatabase.getSongsFromQuery(
                getQueryForSongs(filterList, orderList)
            )
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

    fun getAlarms(): LiveData<List<Alarm>> =
        libraryDatabase.getAlarms().map { list -> list.map { it.toViewAlarm() } }

    suspend fun getAlarmList(): List<Alarm> =
        libraryDatabase.getAlarmList().map { it.toViewAlarm() }

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

    fun getAlbumArtUrl(id: Long) = ampacheDataSource.getAlbumArtUrl(id)
    fun getArtistArtUrl(id: Long) = ampacheDataSource.getArtistArtUrl(id)

    /**
     * Download status
     */

    fun hasDownloaded(song: SongInfo): Boolean =
        downloadManager.downloadIndex.getDownload(ampacheDataSource.getSongUrl(song.id)) != null

    /**
     * Private Method : New data
     */

    private suspend fun newSongs() =
        newList(AmpacheDataSource::getNewSongs) { ampacheSongList ->
            if (ampacheSongList != null) {
                val songs = ampacheSongList.map { it.toDbSong() }
                libraryDatabase.addOrUpdateSongs(songs)
                val songGenres = ampacheSongList.map { it.toDbSongGenres() }.flatten()
                libraryDatabase.addOrUpdateSongGenres(songGenres)
            }
        }

    private suspend fun newGenres() =
        newList(AmpacheDataSource::getNewGenres) { ampacheGenreList ->
            if (ampacheGenreList != null) {
                libraryDatabase.addOrUpdateGenres(ampacheGenreList.map { it.toDbGenre() })
            }
        }

    private suspend fun newArtists() =
        newList(AmpacheDataSource::getNewArtists) { ampacheArtistList ->
            if (ampacheArtistList != null) {
                libraryDatabase.addOrUpdateArtists(ampacheArtistList.map { it.toDbArtist() })
            }
        }

    private suspend fun newAlbums() =
        newList(AmpacheDataSource::getNewAlbums) { ampacheAlbumList ->
            if (ampacheAlbumList != null) {
                libraryDatabase.addOrUpdateAlbums(ampacheAlbumList.map { it.toDbAlbum() })
            }
        }

    private suspend fun newPlaylists() {
        var listOnServer = ampacheDataSource.getNewPlaylists()
        while (listOnServer != null) {
            libraryDatabase.addOrUpdatePlayLists(listOnServer.map { it.toDbPlaylist() })
            for (playlist in listOnServer) {
                retrievePlaylistSongs(playlist.id)
            }
            listOnServer = ampacheDataSource.getNewPlaylists()
        }
    }

    /**
     * Private Method : added data
     */

    private suspend fun addSongs(from: Calendar) =
        updateList(from, AmpacheDataSource::getAddedSongs) { ampacheSongList ->
            if (ampacheSongList != null) {
                val songs = ampacheSongList.map { it.toDbSong() }
                libraryDatabase.addOrUpdateSongs(songs)
                val songGenres = ampacheSongList.map { it.toDbSongGenres() }.flatten()
                libraryDatabase.addOrUpdateSongGenres(songGenres)
            }
        }

    private suspend fun addGenres(from: Calendar) =
        updateList(from, AmpacheDataSource::getAddedGenres) { ampacheGenreList ->
            if (ampacheGenreList != null) {
                libraryDatabase.addOrUpdateGenres(ampacheGenreList.map { it.toDbGenre() })
            }
        }

    private suspend fun addArtists(from: Calendar) =
        updateList(from, AmpacheDataSource::getAddedArtists) { ampacheArtistList ->
            if (ampacheArtistList != null) {
                libraryDatabase.addOrUpdateArtists(ampacheArtistList.map { it.toDbArtist() })
            }
        }

    private suspend fun addAlbums(from: Calendar) =
        updateList(from, AmpacheDataSource::getAddedAlbums) { ampacheAlbumList ->
            if (ampacheAlbumList != null) {
                libraryDatabase.addOrUpdateAlbums(ampacheAlbumList.map { it.toDbAlbum() })
            }
        }

    private suspend fun addPlaylists(from: Calendar) {
        var listOnServer = ampacheDataSource.getAddedPlaylists(from)
        while (listOnServer != null) {
            libraryDatabase.addOrUpdatePlayLists(listOnServer.map { it.toDbPlaylist() })
            for (playlist in listOnServer) {
                retrievePlaylistSongs(playlist.id)
            }
            listOnServer = ampacheDataSource.getNewPlaylists()
        }
    }

    private suspend fun retrievePlaylistSongs(playlistId: Long) {
        libraryDatabase.clearPlaylist(playlistId)
        var listOnServer = ampacheDataSource.getPlaylistsSongs(playlistId)
        while (listOnServer != null) {
            val playlistCount = libraryDatabase.getPlaylistLastOrder(playlistId)
            libraryDatabase.addOrUpdatePlaylistSongs(listOnServer.mapIndexed { index, songId ->
                DbPlaylistSongs(playlistCount + index, songId.id, playlistId)
            })
            listOnServer = ampacheDataSource.getPlaylistsSongs(playlistId)
        }

    }

    /**
     * Private Method : Updated data
     */

    private suspend fun updateSongs(from: Calendar) =
        updateList(from, AmpacheDataSource::getUpdatedSongs) { ampacheSongList ->
            if (ampacheSongList != null) {
                val songs = ampacheSongList.map { it.toDbSong() }
                libraryDatabase.addOrUpdateSongs(songs)
                val songGenres = ampacheSongList.map { it.toDbSongGenres() }.flatten()
                libraryDatabase.addOrUpdateSongGenres(songGenres)
            }
        }

    private suspend fun updateGenres(from: Calendar) =
        updateList(from, AmpacheDataSource::getUpdatedGenres) { ampacheGenreList ->
            if (ampacheGenreList != null) {
                libraryDatabase.addOrUpdateGenres(ampacheGenreList.map { it.toDbGenre() })
            }
        }

    private suspend fun updateArtists(from: Calendar) =
        updateList(from, AmpacheDataSource::getUpdatedArtists) { ampacheArtistList ->
            if (ampacheArtistList != null) {
                libraryDatabase.addOrUpdateArtists(ampacheArtistList.map { it.toDbArtist() })
            }
        }

    private suspend fun updateAlbums(from: Calendar) =
        updateList(from, AmpacheDataSource::getUpdatedAlbums) { ampacheAlbumList ->
            if (ampacheAlbumList != null) {
                libraryDatabase.addOrUpdateAlbums(ampacheAlbumList.map { it.toDbAlbum() })
            }
        }

    private suspend fun updatePlaylists(from: Calendar) {
        var listOnServer = ampacheDataSource.getUpdatedPlaylists(from)
        while (listOnServer != null) {
            libraryDatabase.addOrUpdatePlayLists(listOnServer.map { it.toDbPlaylist() })
            for (playlist in listOnServer) {
                retrievePlaylistSongs(playlist.id)
            }
            listOnServer = ampacheDataSource.getUpdatedPlaylists(from)
        }
    }

    private suspend fun updateDeletedSongs() {
        var listOnServer = ampacheDataSource.getDeletedSongs()
        while (listOnServer != null) {
            libraryDatabase.removeSongs(listOnServer.map { it.toDbSongId() })
            listOnServer = ampacheDataSource.getDeletedSongs()
        }
    }

    /**
     * Private methods: commons
     */

    private suspend fun <SERVER_TYPE> newList(
        getListOnServer: suspend AmpacheDataSource.() -> List<SERVER_TYPE>?,
        saveToDatabase: suspend (List<SERVER_TYPE>?) -> Unit
    ) {
        var listOnServer = ampacheDataSource.getListOnServer()
        while (listOnServer != null) {
            saveToDatabase(listOnServer)
            listOnServer = ampacheDataSource.getListOnServer()
        }
    }

    private suspend fun <SERVER_TYPE> updateList(
        from: Calendar,
        getListOnServer: suspend AmpacheDataSource.(Calendar) -> List<SERVER_TYPE>?,
        saveToDatabase: suspend (List<SERVER_TYPE>?) -> Unit
    ) {
        var listOnServer = ampacheDataSource.getListOnServer(from)
        while (listOnServer != null) {
            saveToDatabase(listOnServer)
            listOnServer = ampacheDataSource.getListOnServer(from)
        }
    }

    private fun <T : Any> convertToPagingLiveData(dataSourceFactory: DataSource.Factory<Int, T>): LiveData<PagingData<T>> =
        Pager(
            PagingConfig(100),
            0,
            dataSourceFactory.asPagingSourceFactory(Dispatchers.IO)
        ).liveData

    private fun getQueryForSongs(filters: List<Filter<*>>, orderList: List<Order>): String {

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
                        Order.Subject.ARTIST -> " artist.name"
                        Order.Subject.ALBUM_ARTIST -> " albumArtist.name"
                        Order.Subject.ALBUM -> " album.name"
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

            if (orderList.isEmpty() || (orderList.size == 1 && orderList[0].orderingType != Order.Ordering.RANDOM) || orderList.any { it.orderingSubject == Order.Subject.ALL }) {
                FirebaseCrashlytics.getInstance()
                    .recordException(Exception("This is not the order you looking for (orderStatement: $orderStatement)"))
            }

            return orderStatement
        }

        return "SELECT song.id FROM song" +
                constructJoinStatement(filters, orderList) +
                constructWhereStatement(filters) +
                constructOrderStatement()
    }

    private fun constructJoinStatement(
        filterList: List<Filter<*>>,
        orderList: List<Order>
    ): String {
        if (filterList.isEmpty() && orderList.isEmpty()) {
            return " "
        }
        val isJoiningArtist =
            filterList.any { it is Filter.Search } || orderList.any { it.orderingSubject == Order.Subject.ARTIST }
        val isJoiningAlbum =
            filterList.any { it is Filter.Search || it is Filter.AlbumArtistIs } || orderList.any { it.orderingSubject == Order.Subject.ALBUM }
        val isJoiningAlbumArtist =
            filterList.any { it is Filter.Search } || orderList.any { it.orderingSubject == Order.Subject.ALBUM_ARTIST }
        val isJoiningSongGenre = filterList.any { it is Filter.GenreIs }
        val isJoiningGenre =
            filterList.any { it is Filter.Search } || orderList.any { it.orderingSubject == Order.Subject.GENRE }
        val isJoiningPlaylistSongs = filterList.any { it is Filter.PlaylistIs }

        var join = ""
        if (isJoiningArtist) {
            join += " JOIN artist ON song.artistId = artist.id"
        }
        if (isJoiningAlbum || isJoiningAlbumArtist) {
            join += " JOIN album ON song.albumId = album.id"
        }
        if (isJoiningAlbumArtist) {
            join += " JOIN artist AS albumArtist ON album.artistId = albumArtist.id"
        }
        if (isJoiningSongGenre) {
            join += " JOIN songgenre ON songgenre.songId = song.id"
        }
        if (isJoiningGenre) {
            join += " JOIN genre ON songgenre.genreId = genre.id"
        }
        if (isJoiningPlaylistSongs) {
            join += " JOIN playlistsongs ON playlistsongs.songId = song.id"
        }

        return join
    }

    private fun constructWhereStatement(filterList: List<Filter<*>>): String {
        var whereStatement = if (filterList.isNotEmpty()) {
            " WHERE"
        } else {
            ""
        }

        filterList.map { it.toDbFilter(DbFilterGroup.CURRENT_FILTER_GROUP_ID) }
            .forEachIndexed { index, filter ->
                whereStatement += when (filter.clause) {
                    DbFilter.SONG_ID,
                    DbFilter.ARTIST_ID,
                    DbFilter.ALBUM_ARTIST_ID,
                    DbFilter.PLAYLIST_ID,
                    DbFilter.ALBUM_ID -> " ${filter.clause} ${filter.argument}"
                    DbFilter.DOWNLOADED -> " ${DbFilter.DOWNLOADED}"
                    DbFilter.NOT_DOWNLOADED -> " ${DbFilter.NOT_DOWNLOADED}"
                    else -> " ${filter.clause} \"${filter.argument}\""
                }
                if (index < filterList.size - 1) {
                    whereStatement += " OR"
                }
            }

        return whereStatement
    }

    suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Boolean =
        libraryDatabase.isPlaylistContainingSong(playlistId, songId)

    companion object {
        private const val LAST_ADD_QUERY = "LAST_ADD_QUERY"
        private const val LAST_UPDATE_QUERY = "LAST_UPDATE_QUERY"
        private const val LAST_CLEAN_QUERY = "LAST_CLEAN_QUERY"
    }
}