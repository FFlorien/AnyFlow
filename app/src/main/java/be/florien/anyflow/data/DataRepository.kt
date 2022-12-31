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
import kotlinx.coroutines.flow.*
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
        convertToPagingLiveData(
            libraryDatabase.getPlaylists().map { it.toViewPlaylist(getPlaylistArtUrl(it.id)) })

    fun <T : Any> getPlaylistSongs(
        playlistId: Long,
        mapping: (DbSongDisplay) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getPlaylistSongs(playlistId).map { mapping(it) })

    /**
     * Searched lists
     */

    fun <T : Any> getAlbumsSearched(
        search: String,
        mapping: (DbAlbumDisplay) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getAlbumsSearched("%$search%").map { mapping(it) })

    suspend fun <T : Any> getAlbumsSearchedList(
        search: String,
        mapping: (DbAlbumDisplay) -> T
    ): List<T> =
        libraryDatabase.getAlbumsSearchedList("%$search%").map { item -> (mapping(item)) }


    fun <T : Any> getAlbumArtistsSearched(
        search: String,
        mapping: (DbArtist) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getAlbumArtistsSearched("%$search%").map { mapping(it) })

    suspend fun <T : Any> getAlbumArtistsSearchedList(
        search: String,
        mapping: (DbArtist) -> T
    ): List<T> =
        libraryDatabase.getAlbumArtistsSearchedList("%$search%").map { item -> (mapping(item)) }

    fun <T : Any> getGenresSearched(
        search: String,
        mapping: (DbGenre) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getGenresSearched("%$search%").map { mapping(it) })

    suspend fun <T : Any> getGenresSearchedList(
        search: String,
        mapping: (DbGenre) -> T
    ): List<T> =
        libraryDatabase.getGenresSearchedList("%$search%").map { item -> (mapping(item)) }

    fun <T : Any> getSongsSearched(
        search: String,
        mapping: (DbSongDisplay) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(libraryDatabase.getSongsSearched("%$search%").map { mapping(it) })

    suspend fun <T : Any> getSongsSearchedList(
        search: String,
        mapping: (DbSongDisplay) -> T
    ): List<T> =
        libraryDatabase.getSongsSearchedList("%$search%").map { item -> (mapping(item)) }

    fun <T : Any> getPlaylistsSearched(
        search: String,
        mapping: (DbPlaylistWithCount) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getPlaylistsSearched("%$search%").map { mapping(it) })

    suspend fun <T : Any> getPlaylistsSearchedList(
        search: String,
        mapping: (DbPlaylistWithCount) -> T
    ): List<T> =
        libraryDatabase.getPlaylistsSearchedList("%$search%").map { item -> (mapping(item)) }

    /**
     * Filtered lists
     */

    fun <T : Any> getAlbumsFiltered(
        filters: List<Filter<*>>,
        mapping: (DbAlbumDisplay) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getAlbumsFiltered(getQueryForAlbumFiltered(filters))
                .map { mapping(it) })

    fun <T : Any> getAlbumArtistsFiltered(
        filters: List<Filter<*>>,
        mapping: (DbArtist) -> T
    ): LiveData<PagingData<T>> = convertToPagingLiveData(
        libraryDatabase.getAlbumArtistsFiltered(getQueryForAlbumArtistFiltered(filters))
            .map { mapping(it) })


    fun <T : Any> getGenresFiltered(
        filters: List<Filter<*>>,
        mapping: (DbGenre) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getGenresFiltered(getQueryForGenreFiltered(filters))
                .map { mapping(it) })

    fun <T : Any> getSongsFiltered(
        filters: List<Filter<*>>,
        mapping: (DbSongDisplay) -> T
    ): LiveData<PagingData<T>> =
        convertToPagingLiveData(
            libraryDatabase.getSongsFiltered(getQueryForSongFiltered(filters)).map { mapping(it) })

    fun <T : Any> getPlaylistsFiltered(
        filters: List<Filter<*>>,
        mapping: (DbPlaylistWithCount) -> T
    ): LiveData<PagingData<T>> = convertToPagingLiveData(
        libraryDatabase.getPlaylistsFiltered(getQueryForPlaylistFiltered(filters))
            .map { mapping(it) })

    /**
     * Playlists modification
     */

    suspend fun createPlaylist(name: String) {
        ampacheDataSource.createPlaylist(name)
        playlists()
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
        .map { filterList ->
            filterList.map { filter ->
                filter.toViewFilter(filterList)
            }
        }

    suspend fun setCurrentFilters(filterList: List<Filter<*>>) = withContext(Dispatchers.IO) {
        libraryDatabase.setCurrentFilters(filterList.map {
            it.toDbFilter(1)
        })
    }

    fun getSongUrl(id: Long) = ampacheDataSource.getSongUrl(id)
    fun getAlbumArtUrl(id: Long) = ampacheDataSource.getArtUrl(ART_TYPE_ALBUM, id)
    fun getArtistArtUrl(id: Long) = ampacheDataSource.getArtUrl(ART_TYPE_ARTIST, id)
    fun getPlaylistArtUrl(id: Long) = ampacheDataSource.getArtUrl(ART_TYPE_PLAYLIST, id)
    fun getArtUrl(type: String, id: Long) = ampacheDataSource.getArtUrl(type, id)

    /**
     * Download status
     */

    fun hasDownloaded(song: SongInfo): Boolean =
        downloadManager.downloadIndex.getDownload(getSongUrl(song.id)) != null

    /**
     * Infos
     */

    suspend fun getFilteredInfo(infoSource: Filter<*>?): FilterCount {
        val filterList = infoSource?.let { listOf(it) } ?: emptyList()
        return libraryDatabase.getCountFromQuery(getQueryForCount(filterList)).toViewFilterCount()
    }

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

    private suspend fun playlists() {
        ampacheDataSource.getPlaylists()
            .flowOn(Dispatchers.IO)
            .onEach { playlistList ->
                libraryDatabase.addOrUpdatePlayLists(playlistList.map { it.toDbPlaylist() })
                for (playlist in playlistList) {
                    libraryDatabase.clearPlaylist(playlist.id)
                    libraryDatabase.addOrUpdatePlaylistSongs(playlist.items.map {
                        it.toDbPlaylistSong(playlist.id)
                    })
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

    private fun getQueryForAlbumFiltered(filterList: List<Filter<*>>) =
        "SELECT DISTINCT album.id, album.name, album.artistId, album.year,album.diskcount, artist.id, artist.name, artist.summary FROM album JOIN artist ON album.artistid = artist.id JOIN song ON song.albumId = album.id" +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList) +
                " ORDER BY album.name COLLATE UNICODE"

    private fun getQueryForAlbumArtistFiltered(filterList: List<Filter<*>>) =
        "SELECT DISTINCT artist.id, artist.name, artist.summary FROM artist JOIN album ON album.artistId = artist.id JOIN song ON song.albumId = album.id" +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList) +
                " ORDER BY artist.name COLLATE UNICODE"

    private fun getQueryForGenreFiltered(filterList: List<Filter<*>>) =
        "SELECT DISTINCT genre.id, genre.name FROM genre JOIN songgenre ON genre.id = songgenre.genreid JOIN song ON song.id = songgenre.songid " +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList) +
                " ORDER BY genre.name COLLATE UNICODE"

    private fun getQueryForSongFiltered(filterList: List<Filter<*>>) =
        "SELECT DISTINCT song.id, song.title, song.artistId, song.albumId, song.track, song.disk, song.time, song.year, song.composer, song.local, song.bars FROM song" +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList) +
                " ORDER BY song.title COLLATE UNICODE"

    private fun getQueryForPlaylistFiltered(filterList: List<Filter<*>>) =
        "SELECT DISTINCT playlist.id, playlist.name, (SELECT COUNT(*) FROM playlistSongs WHERE playlistsongs.playlistId = playlist.id) as songCount FROM playlist JOIN playlistsongs on playlistsongs.playlistid = playlist.id JOIN song ON playlistsongs.songId = song.id" +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList) +
                " ORDER BY playlist.name COLLATE UNICODE"

    private fun getQueryForCount(filterList: List<Filter<*>>) = "SELECT " +
            "SUM(Song.time) AS duration, " +
            "COUNT(DISTINCT SongGenre.genreId) AS genres, " +
            "COUNT(DISTINCT Album.artistid) AS albumArtists, " +
            "COUNT(DISTINCT Song.albumId) AS albums, " +
            "COUNT(DISTINCT Song.artistId) AS artists, " +
            "COUNT(DISTINCT Song.id) AS songs, " +
            "COUNT(DISTINCT PlaylistSongs.playlistId) AS playlists " +
            "FROM Song " +
            "LEFT JOIN SongGenre ON Song.id = SongGenre.songId " +
            "JOIN Album ON Song.albumId = Album.id " +
            "LEFT JOIN PlaylistSongs ON Song.id = PlaylistSongs.songId" +
            constructJoinStatement(filterList) +
            constructWhereStatement(filterList)


    private fun constructJoinStatement(
        filterList: List<Filter<*>>,
        orderList: List<Order> = emptyList()
    ): String {
        if (filterList.isEmpty() && orderList.isEmpty()) {
            return " "
        }
        val isJoiningArtist = orderList.any { it.orderingSubject == Order.Subject.ARTIST }
        val albumJoinCount =
            filterList.countFilter { it.type == Filter.FilterType.ALBUM_ARTIST_IS }
        val isJoiningAlbum = orderList.any { it.orderingSubject == Order.Subject.ALBUM }
        val isJoiningAlbumArtist =
            orderList.any { it.orderingSubject == Order.Subject.ALBUM_ARTIST }
        val isJoiningSongGenre = orderList.any { it.orderingSubject == Order.Subject.GENRE }
        val isJoiningGenreForOrder = orderList.any { it.orderingSubject == Order.Subject.GENRE }
        val songGenreJoinCount = filterList.countFilter { it.type == Filter.FilterType.GENRE_IS }
        val playlistSongsJointCount =
            filterList.countFilter { it.type == Filter.FilterType.PLAYLIST_IS }

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
        if (isJoiningGenreForOrder) {
            join += " JOIN genre ON songgenre.genreId = genre.id"
        }
        for (i in 0 until albumJoinCount) {
            join += " JOIN album AS album$i ON album$i.id = song.albumid"
        }
        for (i in 0 until songGenreJoinCount) {
            join += " JOIN songgenre AS songgenre$i ON songgenre$i.songId = song.id"
        }
        for (i in 0 until playlistSongsJointCount) {
            join += " JOIN playlistsongs AS playlistsongs$i ON playlistsongs$i.songId = song.id"
        }

        return join
    }

    private fun List<Filter<*>>.countFilter(predicate: (Filter<*>) -> Boolean): Int {
        val baseCount = count(predicate)
        var childrenCount = 0
        forEach {
            childrenCount += it.children.countFilter(predicate)
        }
        return baseCount + childrenCount
    }

    private fun constructWhereStatement(filterList: List<Filter<*>>): String {
        var whereStatement = if (filterList.isNotEmpty()) {
            " WHERE"
        } else {
            ""
        }

        whereStatement += constructWhereSubStatement(filterList, 0, 0, 0)

        return whereStatement
    }

    private fun constructWhereSubStatement(
        filterList: List<Filter<*>>,
        genreLevel: Int,
        playlistLevel: Int,
        albumArtistLevel: Int
    ): String {
        var futureGenreLevel = genreLevel
        var futurePlaylistLevel = playlistLevel
        var futureAlbumArtistLevel = albumArtistLevel
        var whereStatement = ""
        filterList
            .forEachIndexed { index, filter ->
                val dbFilter = filter.toDbFilter(DbFilterGroup.CURRENT_FILTER_GROUP_ID)
                if (filter.children.isNotEmpty()) {
                    whereStatement += " ("
                }
                whereStatement += when (dbFilter.clause) {
                    DbFilter.SONG_ID,
                    DbFilter.ARTIST_ID,
                    DbFilter.ALBUM_ID -> " ${dbFilter.clause} ${dbFilter.argument.toLong()}"
                    DbFilter.ALBUM_ARTIST_ID -> {
                        futureAlbumArtistLevel = albumArtistLevel + 1
                        " album${albumArtistLevel}.artistid = ${dbFilter.argument.toLong()}"
                    }
                    DbFilter.GENRE_IS -> {
                        futureGenreLevel = genreLevel + 1
                        " songgenre${genreLevel}.genreid = ${dbFilter.argument.toLong()}"
                    }
                    DbFilter.PLAYLIST_ID -> {
                        futurePlaylistLevel = playlistLevel + 1
                        " playlistsongs${playlistLevel}.playlistid = ${dbFilter.argument.toLong()}"
                    }
                    DbFilter.DOWNLOADED -> " ${DbFilter.DOWNLOADED}"
                    DbFilter.NOT_DOWNLOADED -> " ${DbFilter.NOT_DOWNLOADED}"
                    else -> " ${dbFilter.clause} \"${dbFilter.argument}\""
                }
                if (filter.children.isNotEmpty()) {
                    whereStatement += " AND" + constructWhereSubStatement(
                        filter.children,
                        futureGenreLevel,
                        futurePlaylistLevel,
                        futureAlbumArtistLevel
                    )
                    whereStatement += ")"
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

        const val ART_TYPE_SONG = "song"
        const val ART_TYPE_ALBUM = "album"
        const val ART_TYPE_ARTIST = "artist"
        const val ART_TYPE_PLAYLIST = "playlist"
    }
}