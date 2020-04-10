package be.florien.anyflow.data.local

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import be.florien.anyflow.data.local.dao.*
import be.florien.anyflow.data.local.model.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


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
    val changeUpdater: LiveData<Int> = MutableLiveData()

    /**
     * Getters
     */

    suspend fun getSongAtPosition(position: Int): DbSong? = getSongDao().forPositionInQueue(position)

    suspend fun getPositionForSong(song: DbSongDisplay): Int? = getSongDao().findPositionInQueue(song.id)

    fun getSongsInQueueOrder() = getSongDao().displayInQueueOrder()

    suspend fun getSongsFromQuery(query: String) = getSongDao().forCurrentFilters(SimpleSQLiteQuery(query))

    fun getGenres() = getSongDao().genreOrderByGenre()

    fun getArtists() = getArtistDao().orderByName()

    fun getAlbums(): DataSource.Factory<Int, DbAlbumDisplay> = getAlbumDao().orderByName()

    fun getCurrentFilters(): LiveData<List<DbFilter>> = getFilterDao().currentFilters()

    fun getFilterGroups(): LiveData<List<DbFilterGroup>> = getFilterGroupDao().allSavedFilterGroup()

    suspend fun setSavedGroupAsCurrentFilters(filterGroup: DbFilterGroup) {
        val filterForGroup = getFilterDao().filterForGroup(filterGroup.id)
        setCurrentFilters(filterForGroup)
    }

    fun getOrders(): LiveData<List<DbOrder>> = getOrderDao().all()

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

    suspend fun setCurrentFilters(filters: List<DbFilter>) = asyncUpdate(CHANGE_FILTER_GROUP) {
        val currentFilterGroup = DbFilterGroup(1, "Current Filters")
        getFilterGroupDao().insertSingle(currentFilterGroup)
        getFilterDao().updateGroup(currentFilterGroup, filters.map { it.copy(filterGroup = 1) })
    }

    suspend fun correctAlbumArtist(songs: List<DbSong>) = asyncUpdate(CHANGE_SONGS) {
        songs.distinctBy { it.albumId }
                .forEach { getAlbumDao().updateAlbumArtist(it.albumId, it.albumArtistId, it.albumArtistName) }
    }

    suspend fun createFilterGroup(filters: List<DbFilter>, name: String) = asyncUpdate(CHANGE_FILTER_GROUP) {
        val filterGroup = DbFilterGroup(0, name)
        val newId = getFilterGroupDao().insertSingle(filterGroup)
        val filtersUpdated = filters.map { it.copy(filterGroup = newId) }
        getFilterDao().insert(filtersUpdated)
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

        fun getInstance(context: Context): LibraryDatabase {
            if (instance == null) {
                instance = create(context)
            }
            return instance!!
        }

        @Synchronized
        private fun create(context: Context): LibraryDatabase {
            return Room.databaseBuilder(context, LibraryDatabase::class.java, DB_NAME)
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