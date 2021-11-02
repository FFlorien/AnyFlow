package be.florien.anyflow.data.local

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.DataSource
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import be.florien.anyflow.data.local.dao.*
import be.florien.anyflow.data.local.model.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


@Database(entities = [DbAlbum::class, DbArtist::class, DbPlaylist::class, DbQueueOrder::class, DbSong::class, DbFilter::class, DbFilterGroup::class, DbOrder::class, DbPlaylistSongs::class, DbAlarm::class], version = 6)
abstract class LibraryDatabase : RoomDatabase() {

    protected abstract fun getAlbumDao(): AlbumDao
    protected abstract fun getArtistDao(): ArtistDao
    protected abstract fun getPlaylistDao(): PlaylistDao
    protected abstract fun getPlaylistSongsDao(): PlaylistSongDao
    protected abstract fun getQueueOrderDao(): QueueOrderDao
    protected abstract fun getSongDao(): SongDao
    protected abstract fun getFilterDao(): FilterDao
    protected abstract fun getFilterGroupDao(): FilterGroupDao
    protected abstract fun getOrderDao(): OrderDao
    protected abstract fun getAlarmDao(): AlarmDao
    val changeUpdater: LiveData<Int?> = MutableLiveData()

    /**
     * Getters
     */

    suspend fun getSongAtPosition(position: Int): DbSong? = getSongDao().forPositionInQueue(position)
    suspend fun getPositionForSong(songId: Long): Int? = getSongDao().findPositionInQueue(songId)
    suspend fun getSongById(songId: Long): DbSong? = getSongDao().findById(songId)

    fun getSongsInQueueOrder(): DataSource.Factory<Int, DbSongDisplay> = getSongDao().displayInQueueOrder()
    fun getSongsInAlphabeticalOrder(): DataSource.Factory<Int, DbSongDisplay> = getSongDao().displayInAlphabeticalOrder()
    fun getUrlsInQueueOrder(): LiveData<List<String>> = getSongDao().urlInQueueOrder()
    fun getSongsFiltered(filter: String): DataSource.Factory<Int, DbSongDisplay> = getSongDao().displayFiltered(filter)
    suspend fun getSongsFilteredList(filter: String): List<DbSongDisplay> = getSongDao().displayFilteredList(filter)

    suspend fun getQueueSize(): Int? = getSongDao().queueSize()

    suspend fun getSongsFromQuery(query: String): List<Long> = getSongDao().forCurrentFilters(SimpleSQLiteQuery(query))
    fun searchSongs(filter: String): LiveData<List<Long>> = getSongDao().searchPositionsWhereFilterPresent(filter)

    fun getGenres(): DataSource.Factory<Int, String> = getSongDao().genreOrderByGenre()
    fun getGenresFiltered(filter: String): DataSource.Factory<Int, String> = getSongDao().genreOrderByGenreFiltered(filter)
    suspend fun getGenresFilteredList(filter: String): List<String> = getSongDao().genreOrderByGenreFilteredList(filter)

    fun getAlbumArtists(): DataSource.Factory<Int, DbArtistDisplay> = getArtistDao().orderByName()
    fun getAlbumArtistsFiltered(filter: String): DataSource.Factory<Int, DbArtistDisplay> = getArtistDao().orderByNameFiltered(filter)
    suspend fun getAlbumArtistsFilteredList(filter: String): List<DbArtistDisplay> = getArtistDao().orderByNameFilteredList(filter)

    fun getAlbums(): DataSource.Factory<Int, DbAlbumDisplay> = getAlbumDao().orderByName()
    fun getAlbumsFiltered(filter: String): DataSource.Factory<Int, DbAlbumDisplay> = getAlbumDao().orderByNameFiltered(filter)
    suspend fun getAlbumsFilteredList(filter: String): List<DbAlbumDisplay> = getAlbumDao().orderByNameFilteredList(filter)


    fun getPlaylists(): DataSource.Factory<Int, DbPlaylist> = getPlaylistDao().orderByName()
    fun getPlaylistsFiltered(filter: String): DataSource.Factory<Int, DbPlaylist> = getPlaylistDao().orderByNameFiltered(filter)
    suspend fun getPlaylistsFilteredList(filter: String): List<DbPlaylist> = getPlaylistDao().orderByNameFilteredList(filter)
    suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Boolean = getPlaylistSongsDao().isPlaylistContainingSong(playlistId, songId) > 0

    fun getCurrentFilters(): LiveData<List<DbFilter>> = getFilterDao().currentFilters().distinctUntilChanged()

    fun getFilterGroups(): LiveData<List<DbFilterGroup>> = getFilterGroupDao().allSavedFilterGroup()

    suspend fun setSavedGroupAsCurrentFilters(filterGroup: DbFilterGroup) {
        val filterForGroup = getFilterDao().filterForGroup(filterGroup.id)
        setCurrentFilters(filterForGroup)
    }

    fun getOrders(): LiveData<List<DbOrder>> = getOrderDao().all().distinctUntilChanged()
    fun getOrderList(): List<DbOrder> = getOrderDao().list()

    fun getAlarms(): LiveData<List<DbAlarm>> = getAlarmDao().all()

    /**
     * Getters from raw queries
     */

    suspend fun getSongsFromRawQuery(rawQuery: String) = getSongDao().rawQuery(SimpleSQLiteQuery(rawQuery))
    suspend fun getArtistsFomRawQuery(rawQuery: String) = getArtistDao().rawQuery(SimpleSQLiteQuery(rawQuery))
    suspend fun getAlbumsFomRawQuery(rawQuery: String) = getAlbumDao().rawQuery(SimpleSQLiteQuery(rawQuery))
    suspend fun getPlaylistsFomRawQuery(rawQuery: String) = getPlaylistDao().rawQuery(SimpleSQLiteQuery(rawQuery))
    suspend fun getFilterFomRawQuery(rawQuery: String) = getFilterDao().rawQuery(SimpleSQLiteQuery(rawQuery))
    suspend fun getFilterGroupFomRawQuery(rawQuery: String) = getFilterGroupDao().rawQuery(SimpleSQLiteQuery(rawQuery))
    suspend fun getQueueOrderFromRawQuery(rawQuery: String) = getQueueOrderDao().rawQuery(SimpleSQLiteQuery(rawQuery))
    suspend fun getOrderFromRawQuery(rawQuery: String) = getOrderDao().rawQuery(SimpleSQLiteQuery(rawQuery))

    /**
     * Setters
     */

    suspend fun addSongs(songs: List<DbSong>) = asyncUpdate(CHANGE_SONGS) {
        getSongDao().insert(songs)
    }

    suspend fun addArtists(artists: List<DbArtist>) = asyncUpdate(CHANGE_ARTISTS) {
        getArtistDao().insert(artists)
    }

    suspend fun addAlbums(albums: List<DbAlbum>) = asyncUpdate(CHANGE_ALBUMS) {
        getAlbumDao().insert(albums)
    }

    suspend fun addPlayLists(playlists: List<DbPlaylist>) = asyncUpdate(CHANGE_PLAYLISTS) {
        getPlaylistDao().insert(playlists)
    }

    suspend fun addPlaylistSongs(playlistSongs: List<DbPlaylistSongs>) = asyncUpdate(CHANGE_PLAYLIST_SONG) {
        getPlaylistSongsDao().insert(playlistSongs)
    }

    suspend fun setCurrentFilters(filters: List<DbFilter>) = asyncUpdate(CHANGE_FILTERS) {
        withTransaction {
            getFilterDao().updateGroup(DbFilterGroup.currentFilterGroup, filters.map { it.copy(filterGroup = 1) })
        }
    }

    suspend fun correctAlbumArtist(songs: List<DbSong>) = asyncUpdate(CHANGE_SONGS) {
        songs.distinctBy { it.albumId }
                .forEach { getAlbumDao().updateAlbumArtist(it.albumId, it.albumArtistId, it.albumArtistName) }
    }

    suspend fun createFilterGroup(filters: List<DbFilter>, name: String) = asyncUpdate(CHANGE_FILTER_GROUP) {
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

    suspend fun artForFilters(whereStatement: String) = getSongDao().artForFilters(SimpleSQLiteQuery(getQueryForFiltersArt(whereStatement)))

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

    suspend fun addAlarm(alarm: DbAlarm) {
        getAlarmDao().insertSingle(alarm)
    }

    suspend fun updateAlarm(alarm: DbAlarm) {
        getAlarmDao().update(alarm)
    }

    /**
     * Private methods
     */

    private fun getQueryForFiltersArt(whereStatement: String) = "SELECT DISTINCT art FROM song$whereStatement"

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
        const val CHANGE_ALBUMS = 1
        const val CHANGE_ARTISTS = 2
        const val CHANGE_PLAYLISTS = 3
        const val CHANGE_ORDER = 4
        const val CHANGE_FILTERS = 5
        const val CHANGE_QUEUE = 6
        const val CHANGE_FILTER_GROUP = 7
        const val CHANGE_PLAYLIST_SONG = 8

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
                Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).setTransactionExecutor(Executors.newSingleThreadExecutor())
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
                    .addMigrations(
                            object : Migration(1, 2) {
                                override fun migrate(database: SupportSQLiteDatabase) {
                                    database.execSQL("ALTER TABLE Artist ADD COLUMN art TEXT NOT NULL DEFAULT ''")
                                }

                            },
                            object : Migration(2, 3) {
                                override fun migrate(database: SupportSQLiteDatabase) {
                                    database.execSQL("CREATE TABLE FilterGroup (id INTEGER NOT NULL, name TEXT NOT NULL, PRIMARY KEY(id))")
                                    database.execSQL("ALTER TABLE DbFilter ADD COLUMN filterGroup INTEGER NOT NULL DEFAULT 0 REFERENCES FilterGroup(id) ON DELETE CASCADE")
                                }

                            },
                            object : Migration(3, 4) {
                                override fun migrate(database: SupportSQLiteDatabase) {
                                    database.execSQL("CREATE TABLE PlaylistSongs (songId INTEGER NOT NULL, playlistId INTEGER NOT NULL, PRIMARY KEY(songId, playlistId))")
                                }

                            },
                            object : Migration(4, 5) {
                                override fun migrate(database: SupportSQLiteDatabase) {
                                    database.execSQL("ALTER TABLE DbFilter ADD COLUMN joinClause TEXT DEFAULT null")
                                }

                            },
                            object : Migration(5,6) {
                                override fun migrate(database: SupportSQLiteDatabase) {
                                    database.execSQL("CREATE TABLE Alarm (id INTEGER NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, active INTEGER NOT NULL, monday INTEGER NOT NULL, tuesday INTEGER NOT NULL, wednesday INTEGER NOT NULL, thursday INTEGER NOT NULL, friday INTEGER NOT NULL, saturday INTEGER NOT NULL , sunday INTEGER NOT NULL , PRIMARY KEY(id))")
                                }

                            })
                    .build()
        }

    }
}