package be.florien.anyflow.data.local

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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


@Database(entities = [DbAlbum::class, DbArtist::class, DbPlaylist::class, DbQueueOrder::class, DbSong::class, DbFilter::class, DbFilterGroup::class, DbOrder::class], version = 3)
abstract class LibraryDatabase : RoomDatabase() {

    protected abstract fun getAlbumDao(): AlbumDao
    protected abstract fun getArtistDao(): ArtistDao
    protected abstract fun getPlaylistDao(): PlaylistDao
    protected abstract fun getQueueOrderDao(): QueueOrderDao
    protected abstract fun getSongDao(): SongDao
    protected abstract fun getFilterDao(): FilterDao
    protected abstract fun getFilterGroupDao(): FilterGroupDao
    protected abstract fun getOrderDao(): OrderDao
    val changeUpdater: LiveData<Int?> = MutableLiveData()

    /**
     * Getters
     */

    suspend fun getSongAtPosition(position: Int): DbSong? = getSongDao().forPositionInQueue(position)
    suspend fun getPositionForSong(song: DbSongDisplay): Int? = getSongDao().findPositionInQueue(song.id)

    fun getSongsInQueueOrder(): DataSource.Factory<Int, DbSongDisplay> = getSongDao().displayInQueueOrder()
    fun getUrlsInQueueOrder(): LiveData<List<String>> = getSongDao().urlInQueueOrder()

    suspend fun getQueueSize(): Int? = getSongDao().queueSize()

    suspend fun getSongsFromQuery(query: String): List<Long> = getSongDao().forCurrentFilters(SimpleSQLiteQuery(query))

    fun getGenres(): LiveData<List<String>> = getSongDao().genreOrderByGenre()
    fun getGenresFiltered(filter: String): LiveData<List<String>> = getSongDao().genreOrderByGenreFiltered(filter)

    fun getAlbumArtists(): LiveData<List<DbArtistDisplay>> = getArtistDao().orderByName()
    fun getAlbumArtistsFiltered(filter: String): LiveData<List<DbArtistDisplay>> = getArtistDao().orderByNameFiltered(filter)

    fun getAlbums(): LiveData<List<DbAlbumDisplay>> = getAlbumDao().orderByName()
    fun getAlbumsFiltered(filter: String): LiveData<List<DbAlbumDisplay>> = getAlbumDao().orderByNameFiltered(filter)

    fun getCurrentFilters(): LiveData<List<DbFilter>> = getFilterDao().currentFilters()

    fun getFilterGroups(): LiveData<List<DbFilterGroup>> = getFilterGroupDao().allSavedFilterGroup()

    suspend fun setSavedGroupAsCurrentFilters(filterGroup: DbFilterGroup) {
        val filterForGroup = getFilterDao().filterForGroup(filterGroup.id)
        setCurrentFilters(filterForGroup)
    }

    fun getOrders(): LiveData<List<DbOrder>> = getOrderDao().all()

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

                            })
                    .build()
        }

    }
}