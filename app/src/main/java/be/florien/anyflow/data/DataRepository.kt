package be.florien.anyflow.data

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.paging.*
import androidx.room.withTransaction
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.QueryComposer
import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.server.AmpacheEditSource
import be.florien.anyflow.data.view.*
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
class DataRepository
@Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val ampacheDataSource: AmpacheDataSource,
    private val ampacheEditSource: AmpacheEditSource,
    private val sharedPreferences: SharedPreferences
) {


    private val queryComposer = QueryComposer()
    val changeUpdater: LiveData<Int?> = MutableLiveData()

    /**
     * Getter with server updates
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
     * Songs related methods
     */

    suspend fun getSongAtPosition(position: Int) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getSongDao().forPositionInQueue(position)?.toViewSongInfo()
        }

    suspend fun getPositionForSong(songId: Long) =
        withContext(Dispatchers.IO) { libraryDatabase.getSongDao().findPositionInQueue(songId) }

    fun getSongsInQueueOrder() =
        convertToPagingLiveData(
            libraryDatabase.getSongDao().displayInQueueOrder().map { it.toViewSongInfo() })

    fun getIdsInQueueOrder() = libraryDatabase.getSongDao().songsInQueueOrder()

    fun searchSongs(filter: String) =
        libraryDatabase.getSongDao().searchPositionsWhereFilterPresent("%$filter%")

    suspend fun updateSongLocalUri(songId: Long, uri: String?) {
        libraryDatabase.getSongDao().updateWithLocalUri(songId, uri)
    }

    /**
     * Lists
     */

    suspend fun getQueueSize(): Int? =
        withContext(Dispatchers.IO) { libraryDatabase.getSongDao().queueSize() }

    fun <T : Any> getAlbums(
        mapping: (DbAlbumDisplayForRaw) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getAlbumDao().rawQueryPaging(
                queryComposer.getQueryForAlbumFiltered(filters, search)
            ).map { mapping(it) })

    fun <T : Any> getAlbumArtists(
        mapping: (DbArtist) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getArtistDao().rawQueryPaging(
                queryComposer.getQueryForAlbumArtistFiltered(filters, search)
            ).map { mapping(it) })

    fun <T : Any> getArtists(
        mapping: (DbArtist) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getArtistDao().rawQueryPaging(
                queryComposer.getQueryForArtistFiltered(filters, search)
            ).map { mapping(it) })

    fun <T : Any> getGenres(
        mapping: (DbGenre) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getGenreDao().rawQueryPaging(
                queryComposer.getQueryForGenreFiltered(filters, search)
            ).map { mapping(it) })

    fun <T : Any> getSongs(
        mapping: (DbSongDisplay) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getSongDao().rawQueryPaging(
                queryComposer.getQueryForSongFiltered(filters, search)
            ).map { mapping(it) })

    fun <T : Any> getPlaylists(
        mapping: (DbPlaylistWithCount) -> T,
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getPlaylistDao().rawQueryPaging(
                queryComposer.getQueryForPlaylistFiltered(filters, search)
            ).map { mapping(it) })

    fun getAllPlaylists(): LiveData<PagingData<Playlist>> =
        getPlaylists({ it.toViewPlaylist(getPlaylistArtUrl(it.id)) }, null, null)

    suspend fun getPlaylistsWithSongPresence(songId: Long): List<Long> =
        libraryDatabase.getPlaylistDao().getPlaylistsWithCountAndSongPresence(songId)

    fun <T : Any> getPlaylistSongs(
        playlistId: Long,
        mapping: (DbSongDisplay) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getPlaylistSongsDao().songsFromPlaylist(playlistId).map { mapping(it) })

    suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Boolean =
        libraryDatabase.getPlaylistSongsDao().isPlaylistContainingSong(playlistId, songId) > 0

    suspend fun <T : Any> getAlbumsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbAlbumDisplayForRaw) -> T,
    ): List<T> =
        libraryDatabase.getAlbumDao().rawQueryList(
            queryComposer.getQueryForAlbumFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getAlbumArtistsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbArtist) -> T
    ): List<T> =
        libraryDatabase.getArtistDao().rawQuery(
            queryComposer.getQueryForAlbumArtistFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getArtistsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbArtist) -> T
    ): List<T> =
        libraryDatabase.getArtistDao().rawQuery(
            queryComposer.getQueryForArtistFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getGenresSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbGenre) -> T
    ): List<T> =
        libraryDatabase.getGenreDao().rawQuery(
            queryComposer.getQueryForGenreFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getSongsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbSongDisplay) -> T
    ): List<T> =
        libraryDatabase.getSongDao().rawQueryList(
            queryComposer.getQueryForSongFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    suspend fun <T : Any> getPlaylistsSearchedList(
        filters: List<Filter<*>>?,
        search: String,
        mapping: (DbPlaylistWithCount) -> T
    ): List<T> =
        libraryDatabase.getPlaylistDao().rawQueryList(
            queryComposer.getQueryForPlaylistFiltered(filters, search)
        ).map { item -> (mapping(item)) }

    fun getSong(id: Long): LiveData<SongInfo> =
        libraryDatabase.getSongDao().findById(id).map { it.toViewSongInfo() }

    suspend fun getSongSync(id: Long): SongInfo =
        libraryDatabase.getSongDao().findByIdSync(id).toViewSongInfo()

    /**
     * Playlists modification
     */

    suspend fun createPlaylist(name: String) {
        ampacheEditSource.createPlaylist(name)
        playlists()
    }

    suspend fun deletePlaylist(id: Long) {
        ampacheEditSource.deletePlaylist(id)
        libraryDatabase.getPlaylistDao().delete(DbPlaylist(id, "", ""))
    }

    suspend fun addSongToPlaylist(songId: Long, playlistId: Long) {
        ampacheEditSource.addSongToPlaylist(songId, playlistId)
        val playlistLastOrder =
            libraryDatabase.getPlaylistSongsDao().playlistLastOrder(playlistId) ?: -1
        asyncUpdate(CHANGE_PLAYLISTS) {
            libraryDatabase.getPlaylistSongsDao().upsert(
                listOf(
                    DbPlaylistSongs(
                        playlistLastOrder + 1,
                        songId,
                        playlistId
                    )
                )
            )
        }
    }

    suspend fun removeSongFromPlaylist(songId: Long, playlistId: Long) {
        ampacheEditSource.removeSongFromPlaylist(playlistId, songId)
        libraryDatabase.getPlaylistSongsDao().delete(DbPlaylistSongs(0, songId, playlistId))
    }

    /**
     * Orders
     */

    fun getOrders() =
        libraryDatabase.getOrderDao().all().distinctUntilChanged().map { list -> list.map { item -> item.toViewOrder() } }

    suspend fun getOrderlessQueue(filterList: List<Filter<*>>, orderList: List<Order>): List<Long> =
        withContext(Dispatchers.IO) {
            libraryDatabase.getSongDao().forCurrentFilters(
                queryComposer.getQueryForSongs(filterList, orderList)
            )
        }

    suspend fun setOrders(orders: List<Order>) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getOrderDao().replaceBy(orders.map { it.toDbOrder() })
        }

    suspend fun saveQueueOrder(listToSave: MutableList<Long>) {
        libraryDatabase.getQueueOrderDao()
            .setOrder(listToSave.mapIndexed { index, id -> DbQueueOrder(index, id) })
    }

    /**
     * Alarms
     */

    suspend fun addAlarm(alarm: Alarm) =
        libraryDatabase.getAlarmDao().insertSingle(alarm.toDbAlarm())

    fun getAlarms(): LiveData<List<Alarm>> =
        libraryDatabase.getAlarmDao().all().map { list -> list.map { it.toViewAlarm() } }

    suspend fun getAlarmList(): List<Alarm> =
        libraryDatabase.getAlarmDao().list().map { it.toViewAlarm() }

    suspend fun activateAlarm(alarm: Alarm) {
        val newAlarm = alarm.copy(active = true)
        libraryDatabase.getAlarmDao().update(newAlarm.toDbAlarm())
    }

    suspend fun deactivateAlarm(alarm: Alarm) {
        val newAlarm = alarm.copy(active = false)
        libraryDatabase.getAlarmDao().update(newAlarm.toDbAlarm())
    }

    suspend fun editAlarm(alarm: Alarm) {
        libraryDatabase.getAlarmDao().update(alarm.toDbAlarm())
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        libraryDatabase.getAlarmDao().delete(alarm.toDbAlarm())
    }

    /**
     * Filter groups
     */

    suspend fun createFilterGroup(filterList: List<Filter<*>>, name: String) =
        withContext(Dispatchers.IO) {
            if (libraryDatabase.getFilterGroupDao().withNameIgnoreCase(name).isEmpty()) {
                val filterGroup = DbFilterGroup(0, name)
                val newId = libraryDatabase.getFilterGroupDao().insertSingle(filterGroup)
                val filtersUpdated = filterList.map { it.toDbFilter(newId) }
                libraryDatabase.getFilterDao().insert(filtersUpdated)
            } else {
                throw IllegalArgumentException("A filter group with this name already exists")
            }
        }

    fun getFilterGroups() = libraryDatabase.getFilterGroupDao().allSavedFilterGroup()
        .map { groupList -> groupList.map { it.toViewFilterGroup() } }

    suspend fun setSavedGroupAsCurrentFilters(filterGroup: FilterGroup) =
        withContext(Dispatchers.IO) {
            libraryDatabase.apply {
                withTransaction {
                    val filterForGroup = getFilterDao().filterForGroup(filterGroup.id)
                    getFilterDao().updateGroup(
                        DbFilterGroup.currentFilterGroup,
                        filterForGroup.map { it.copy(filterGroup = 1) })
                }
            }
        }

    fun getCurrentFilters(): LiveData<List<Filter<*>>> =
        libraryDatabase.getFilterDao().currentFilters().distinctUntilChanged()
            .map { filterList ->
                filterList.mapNotNull { filter ->
                    if (filter.parentFilter != null) {
                        null
                    } else {
                        filter.toViewFilter(filterList)
                    }
                }
            }

    suspend fun setCurrentFilters(filterList: List<Filter<*>>) = withContext(Dispatchers.IO) {
        libraryDatabase.apply {
            withTransaction {
                getFilterDao().deleteGroupSync(DbFilterGroup.CURRENT_FILTER_GROUP_ID)
                insertCurrentFilterAndChildren(filterList)
            }
        }
    }

    private suspend fun insertCurrentFilterAndChildren(
        filters: List<Filter<*>>,
        parentId: Long? = null
    ) {
        filters.forEach { filter ->
            val id = libraryDatabase.getFilterDao().insertSingle(filter.toDbFilter(1, parentId))
            insertCurrentFilterAndChildren(filter.children, id)
        }
    }

    fun getSongUrl(id: Long) = ampacheDataSource.getSongUrl(id)
    fun getAlbumArtUrl(id: Long) = ampacheDataSource.getArtUrl(ART_TYPE_ALBUM, id)
    fun getArtistArtUrl(id: Long) = ampacheDataSource.getArtUrl(ART_TYPE_ARTIST, id)
    fun getPlaylistArtUrl(id: Long) = ampacheDataSource.getArtUrl(ART_TYPE_PLAYLIST, id)
    fun getArtUrl(type: String, id: Long) = ampacheDataSource.getArtUrl(type, id)

    /**
     * Infos
     */

    suspend fun getFilteredInfo(infoSource: Filter<*>?): FilterCount {
        val filterList = infoSource?.let { listOf(it) } ?: emptyList()
        return libraryDatabase.getFilterDao().getCount(queryComposer.getQueryForCount(filterList))
            .toViewFilterCount()
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

    private suspend fun playlists() {
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

    private fun <T : Any> convertToPagingLiveData(dataSourceFactory: DataSource.Factory<Int, T>): LiveData<PagingData<T>> =
        Pager(
            PagingConfig(100),
            0,
            dataSourceFactory.asPagingSourceFactory(Dispatchers.IO)
        ).liveData

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