package be.florien.anyflow.data.local

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.paging.DataSource
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import be.florien.anyflow.data.local.dao.*
import be.florien.anyflow.data.local.model.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


@Database(
    version = 1,
    entities = [DbAlbum::class, DbArtist::class, DbPlaylist::class, DbQueueOrder::class, DbSong::class, DbGenre::class, DbSongGenre::class, DbFilter::class, DbFilterGroup::class, DbOrder::class, DbPlaylistSongs::class, DbAlarm::class]
)
abstract class LibraryDatabase : RoomDatabase() {

    protected abstract fun getAlbumDao(): AlbumDao
    protected abstract fun getArtistDao(): ArtistDao
    protected abstract fun getPlaylistDao(): PlaylistDao
    protected abstract fun getPlaylistSongsDao(): PlaylistSongDao
    protected abstract fun getQueueOrderDao(): QueueOrderDao
    protected abstract fun getSongDao(): SongDao
    protected abstract fun getGenreDao(): GenreDao
    protected abstract fun getSongGenreDao(): SongGenreDao
    protected abstract fun getFilterDao(): FilterDao
    protected abstract fun getFilterGroupDao(): FilterGroupDao
    protected abstract fun getOrderDao(): OrderDao
    protected abstract fun getAlarmDao(): AlarmDao
    val changeUpdater: LiveData<Int?> = MutableLiveData()

    /**
     * Getters
     */

    suspend fun getSongCount() = getSongDao().songCount()

    suspend fun getSongAtPosition(position: Int): DbSongDisplay? =
        getSongDao().forPositionInQueue(position)

    suspend fun getPositionForSong(songId: Long): Int? = getSongDao().findPositionInQueue(songId)

    fun getSongsInQueueOrder(): DataSource.Factory<Int, DbSongDisplay> =
        getSongDao().displayInQueueOrder()

    fun getSongsInAlphabeticalOrder(): DataSource.Factory<Int, DbSongDisplay> =
        getSongDao().displayInAlphabeticalOrder()

    fun getIdsInQueueOrder(): LiveData<List<DbSongToPlay>> = getSongDao().songsInQueueOrder()
    fun getSongsFiltered(filter: String): DataSource.Factory<Int, DbSongDisplay> =
        getSongDao().displayFiltered(filter)

    suspend fun getSongsFilteredList(filter: String): List<DbSongDisplay> =
        getSongDao().displayFilteredList(filter)

    suspend fun getQueueSize(): Int? = getSongDao().queueSize()

    suspend fun getSongsFromQuery(query: String): List<Long> =
        getSongDao().forCurrentFilters(SimpleSQLiteQuery(query))

    fun searchSongs(filter: String): LiveData<List<Long>> =
        getSongDao().searchPositionsWhereFilterPresent(filter)

    fun getGenres(): DataSource.Factory<Int, DbGenre> = getGenreDao().genreOrderByGenre()
    fun getGenresFiltered(filter: String): DataSource.Factory<Int, DbGenre> =
        getGenreDao().genreOrderByGenreFiltered(filter)

    suspend fun getGenresFilteredList(filter: String): List<DbGenre> =
        getGenreDao().genreOrderByGenreFilteredList(filter)

    fun getAlbumArtists(): DataSource.Factory<Int, DbArtist> = getArtistDao().albumArtistOrderByName()
    fun getAlbumArtistsFiltered(filter: String): DataSource.Factory<Int, DbArtist> =
        getArtistDao().albumArtistOrderByNameFiltered(filter)

    suspend fun getAlbumArtistsFilteredList(filter: String): List<DbArtist> =
        getArtistDao().albumArtistOrderByNameFilteredList(filter)

    fun getAlbums(): DataSource.Factory<Int, DbAlbumDisplay> = getAlbumDao().orderByName()
    fun getAlbumsFiltered(filter: String): DataSource.Factory<Int, DbAlbumDisplay> =
        getAlbumDao().orderByNameFiltered(filter)

    suspend fun getAlbumsFilteredList(filter: String): List<DbAlbumDisplay> =
        getAlbumDao().orderByNameFilteredList(filter)


    fun getPlaylists(): DataSource.Factory<Int, DbPlaylist> = getPlaylistDao().orderByName()
    fun getPlaylistsFiltered(filter: String): DataSource.Factory<Int, DbPlaylist> =
        getPlaylistDao().orderByNameFiltered(filter)

    suspend fun getPlaylistsFilteredList(filter: String): List<DbPlaylist> =
        getPlaylistDao().orderByNameFilteredList(filter)

    suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Boolean =
        getPlaylistSongsDao().isPlaylistContainingSong(playlistId, songId) > 0

    fun getCurrentFilters(): LiveData<List<DbFilter>> =
        getFilterDao().currentFilters().distinctUntilChanged()

    fun getFilterGroups(): LiveData<List<DbFilterGroup>> = getFilterGroupDao().allSavedFilterGroup()

    suspend fun setSavedGroupAsCurrentFilters(filterGroup: DbFilterGroup) {
        val filterForGroup = getFilterDao().filterForGroup(filterGroup.id)
        setCurrentFilters(filterForGroup)
    }

    fun getOrders(): LiveData<List<DbOrder>> = getOrderDao().all().distinctUntilChanged()

    fun getAlarms(): LiveData<List<DbAlarm>> = getAlarmDao().all()
    suspend fun getAlarmList(): List<DbAlarm> = getAlarmDao().list()

    /**
     * Getters from raw queries
     */

    suspend fun getSongsFromRawQuery(rawQuery: String) =
        getSongDao().rawQuery(SimpleSQLiteQuery(rawQuery))

    suspend fun getArtistsFomRawQuery(rawQuery: String) =
        getArtistDao().rawQuery(SimpleSQLiteQuery(rawQuery))

    suspend fun getAlbumsFomRawQuery(rawQuery: String) =
        getAlbumDao().rawQuery(SimpleSQLiteQuery(rawQuery))

    suspend fun getPlaylistsFomRawQuery(rawQuery: String) =
        getPlaylistDao().rawQuery(SimpleSQLiteQuery(rawQuery))

    suspend fun getFilterFomRawQuery(rawQuery: String) =
        getFilterDao().rawQuery(SimpleSQLiteQuery(rawQuery))

    suspend fun getFilterGroupFomRawQuery(rawQuery: String) =
        getFilterGroupDao().rawQuery(SimpleSQLiteQuery(rawQuery))

    suspend fun getQueueOrderFromRawQuery(rawQuery: String) =
        getQueueOrderDao().rawQuery(SimpleSQLiteQuery(rawQuery))

    suspend fun getOrderFromRawQuery(rawQuery: String) =
        getOrderDao().rawQuery(SimpleSQLiteQuery(rawQuery))

    /**
     * Setters
     */

    suspend fun addOrUpdateSongs(songs: List<DbSong>) = asyncUpdate(CHANGE_SONGS) {
        getSongDao().upsert(songs)
    }

    suspend fun addOrUpdateGenres(genres: List<DbGenre>) = asyncUpdate(CHANGE_GENRES) {
        getGenreDao().upsert(genres)
    }

    suspend fun addOrUpdateSongGenres(genres: List<DbSongGenre>) = asyncUpdate(CHANGE_GENRES) {
        getSongGenreDao().upsert(genres)
    }

    suspend fun addOrUpdateArtists(artists: List<DbArtist>) = asyncUpdate(CHANGE_ARTISTS) {
        getArtistDao().upsert(artists)
    }

    suspend fun addOrUpdateAlbums(albums: List<DbAlbum>) = asyncUpdate(CHANGE_ALBUMS) {
        getAlbumDao().upsert(albums)
    }

    suspend fun addOrUpdatePlayLists(playlists: List<DbPlaylist>) = asyncUpdate(CHANGE_PLAYLISTS) {
        getPlaylistDao().upsert(playlists)
    }

    suspend fun addOrUpdatePlaylistSongs(playlistSongs: List<DbPlaylistSongs>) =
        asyncUpdate(CHANGE_PLAYLIST_SONG) {
            getPlaylistSongsDao().upsert(playlistSongs)
        }

    suspend fun updateSongLocalUri(songId: Long, uri: String) {
        getSongDao().updateWithLocalUri(songId, uri)
    }

    suspend fun setCurrentFilters(filters: List<DbFilter>) = asyncUpdate(CHANGE_FILTERS) {
        withTransaction {
            getFilterDao().updateGroup(
                DbFilterGroup.currentFilterGroup,
                filters.map { it.copy(filterGroup = 1) })
        }
    }

    suspend fun createFilterGroup(filters: List<DbFilter>, name: String) =
        asyncUpdate(CHANGE_FILTER_GROUP) {
            if (getFilterGroupDao().withNameIgnoreCase(name).isEmpty()) {
                val filterGroup = DbFilterGroup(0, name)
                val newId = getFilterGroupDao().insertSingle(filterGroup)
                val filtersUpdated = filters.map { it.copy(filterGroup = newId) }
                getFilterDao().insert(filtersUpdated)
            } else {
                throw IllegalArgumentException("A filter group with this name already exists")
            }
        }

    suspend fun filterForGroupSync(id: Long) = getFilterDao().filterForGroup(id)

    suspend fun setOrders(orders: List<DbOrder>) = asyncUpdate(CHANGE_ORDER) {
        getOrderDao().replaceBy(orders)
    }

    suspend fun saveQueueOrder(it: List<Long>) {
        val queueOrder = mutableListOf<DbQueueOrder>()
        it.forEachIndexed { index, songId ->
            queueOrder.add(DbQueueOrder(index, songId))
        }
        asyncUpdate(CHANGE_QUEUE) { getQueueOrderDao().setOrder(queueOrder) }
    }

    suspend fun addAlarm(alarm: DbAlarm) = getAlarmDao().insertSingle(alarm)

    suspend fun updateAlarm(alarm: DbAlarm) {
        getAlarmDao().update(alarm)
    }

    suspend fun deleteAlarm(alarm: DbAlarm) {
        getAlarmDao().delete(alarm)
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

    suspend fun removeSongs(idsToDelete: List<DbSongId>) {
        getSongDao().deleteWithId(idsToDelete)
    }

    suspend fun clearPlaylist(playlistId: Long) {
        getPlaylistSongsDao().deleteSongsFromPlaylist(playlistId)
    }

    companion object {
        const val CHANGE_SONGS = 0
        const val CHANGE_ALBUMS = 1
        const val CHANGE_ARTISTS = 2
        const val CHANGE_PLAYLISTS = 3
        const val CHANGE_ORDER = 4
        const val CHANGE_FILTERS = 5
        const val CHANGE_QUEUE = 6
        const val CHANGE_FILTER_GROUP = 7
        const val CHANGE_PLAYLIST_SONG = 8
        const val CHANGE_GENRES = 9

        @Volatile
        private var instance: LibraryDatabase? = null
        private const val DB_NAME = "anyflow.db"

        fun getInstance(context: Context, isForTests: Boolean = false): LibraryDatabase {
            if (instance == null) {
                if (!isForTests) {
                    instance = create(context, isForTests)
                } else {
                    return create(context, isForTests)
                }
            }
            return instance!!
        }

        @Synchronized
        private fun create(context: Context, isForTests: Boolean): LibraryDatabase {
            val databaseBuilder = if (isForTests) {
                Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java)
                    .setTransactionExecutor(Executors.newSingleThreadExecutor())
            } else {
                Room.databaseBuilder(context, LibraryDatabase::class.java, DB_NAME)
            }
            return databaseBuilder
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val currentFilterGroup = DbFilterGroup.currentFilterGroup
                        db.execSQL("INSERT INTO FilterGroup VALUES (${currentFilterGroup.id}, \"${currentFilterGroup.name}\")")
                    }
                })
                .build()
        }
    }
}