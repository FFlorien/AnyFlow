package be.florien.anyflow.data.local

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
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
import be.florien.anyflow.data.toDbFilter
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.Order
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


@Database(
    version = 5,
    entities = [DbAlbum::class, DbArtist::class, DbPlaylist::class, DbQueueOrder::class, DbSong::class, DbGenre::class, DbSongGenre::class, DbFilter::class, DbFilterGroup::class, DbOrder::class, DbPlaylistSongs::class, DbAlarm::class],
    exportSchema = false //todo ?
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
    val queryComposer = QueryComposer()

    /**
     * Getters
     */

    suspend fun getSongCount() = getSongDao().songCount()

    suspend fun getSongAtPosition(position: Int): DbSongDisplay? =
        getSongDao().forPositionInQueue(position)

    suspend fun getPositionForSong(songId: Long): Int? = getSongDao().findPositionInQueue(songId)

    fun getSongsInQueueOrder(): DataSource.Factory<Int, DbSongDisplay> =
        getSongDao().displayInQueueOrder()

    fun getSongsForQuery(
        filters: List<Filter<*>>?,
        search: String?): DataSource.Factory<Int, DbSongDisplay> =
        getSongDao().rawQueryPaging(queryComposer.getQueryForSongFiltered(filters, search))

    fun getIdsInQueueOrder(): LiveData<List<DbSongToPlay>> = getSongDao().songsInQueueOrder()

    suspend fun getSongsListForQuery(
        filters: List<Filter<*>>?,
        search: String?): List<DbSongDisplay> =
        getSongDao().rawQueryList(queryComposer.getQueryForSongFiltered(filters, search))

    suspend fun getQueueSize(): Int? = getSongDao().queueSize()

    suspend fun getSongsFromQuery(filterList: List<Filter<*>>, orderList: List<Order>): List<Long> =
        getSongDao().forCurrentFilters(queryComposer.getQueryForSongs(filterList, orderList))

    suspend fun getCountFromQuery(filters: List<Filter<*>>): DbFilterCount =
        getFilterDao().getCount(queryComposer.getQueryForCount(filters))

    fun searchSongs(filter: String): LiveData<List<Long>> =
        getSongDao().searchPositionsWhereFilterPresent(filter)

    suspend fun getWaveForm(songId: Long): DoubleArray =
        getSongDao().getWaveForm(songId).downSamplesArray

    suspend fun getSongDuration(songId: Long): Int = getSongDao().getSongDuration(songId)

    fun getGenresForQuery(
        filters: List<Filter<*>>?,
        search: String?): DataSource.Factory<Int, DbGenre> =
        getGenreDao().rawQueryPaging(queryComposer.getQueryForGenreFiltered(filters, search))

    suspend fun getGenresListForQuery(
        filters: List<Filter<*>>?,
        search: String?): List<DbGenre> =
        getGenreDao().rawQuery(queryComposer.getQueryForGenreFiltered(filters, search))

    fun getArtists(
        filters: List<Filter<*>>?,
        search: String?): DataSource.Factory<Int, DbArtist> =
        getArtistDao().rawQueryPaging(queryComposer.getQueryForArtistFiltered(filters, search))

    suspend fun getArtistsListForQuery(
        filters: List<Filter<*>>?,
        search: String?): List<DbArtist> =
        getArtistDao().rawQuery(queryComposer.getQueryForArtistFiltered(filters, search))

    fun getAlbumArtists(
        filters: List<Filter<*>>?,
        search: String?): DataSource.Factory<Int, DbArtist> =
        getArtistDao().rawQueryPaging(queryComposer.getQueryForAlbumArtistFiltered(filters, search))

    suspend fun getAlbumArtistsListForQuery(
        filters: List<Filter<*>>?,
        search: String?): List<DbArtist> =
        getArtistDao().rawQuery(queryComposer.getQueryForAlbumArtistFiltered(filters, search))

    fun getAlbums(
        filters: List<Filter<*>>?,
        search: String?): DataSource.Factory<Int, DbAlbumDisplayForRaw> =
        getAlbumDao().rawQueryPaging(queryComposer.getQueryForAlbumFiltered(filters, search))

    suspend fun getAlbumsListForQuery(
        filters: List<Filter<*>>?,
        search: String?): List<DbAlbumDisplayForRaw> =
        getAlbumDao().rawQueryList(queryComposer.getQueryForAlbumFiltered(filters, search))

    fun getPlaylists(
        filters: List<Filter<*>>?,
        search: String?): DataSource.Factory<Int, DbPlaylistWithCount> =
        getPlaylistDao().rawQueryPaging(queryComposer.getQueryForPlaylistFiltered(filters, search))

    suspend fun getPlaylistsWithSongPresence(songId: Long): List<Long> =
        getPlaylistDao().getPlaylistsWithCountAndSongPresence(songId)

    suspend fun getPlaylistsSearchedList(
        filters: List<Filter<*>>?,
        search: String?): List<DbPlaylistWithCount> =
        getPlaylistDao().rawQueryList(queryComposer.getQueryForPlaylistFiltered(filters, search))

    suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Boolean =
        getPlaylistSongsDao().isPlaylistContainingSong(playlistId, songId) > 0

    suspend fun getPlaylistLastOrder(playlistId: Long) =
        getPlaylistSongsDao().playlistLastOrder(playlistId) ?: -1

    fun getPlaylistSongs(playlistId: Long) = getPlaylistSongsDao().songsFromPlaylist(playlistId)

    fun getCurrentFilters(): LiveData<List<DbFilter>> =
        getFilterDao().currentFilters().distinctUntilChanged()

    fun getFilterGroups(): LiveData<List<DbFilterGroup>> = getFilterGroupDao().allSavedFilterGroup()

    suspend fun setSavedGroupAsCurrentFilters(filterGroup: DbFilterGroup) {
        withTransaction {
            val filterForGroup = getFilterDao().filterForGroup(filterGroup.id)
            getFilterDao().updateGroup(
                DbFilterGroup.currentFilterGroup,
                filterForGroup.map { it.copy(filterGroup = 1) })
        }
    }

    fun getOrders(): LiveData<List<DbOrder>> = getOrderDao().all().distinctUntilChanged()

    fun getAlarms(): LiveData<List<DbAlarm>> = getAlarmDao().all()
    suspend fun getAlarmList(): List<DbAlarm> = getAlarmDao().list()

    /**
     * Getters from raw queries (Was it only for tests ?)
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

    suspend fun updateWaveForm(songId: Long, waveForm: DoubleArray) {
        val stringify =
            waveForm.takeIf { it.isNotEmpty() }?.joinToString(separator = "|") { "%.3f".format(it) }
        getSongDao().updateWithNewWaveForm(songId, stringify)
    }

    suspend fun setCurrentFilters(filters: List<Filter<*>>) = asyncUpdate(CHANGE_FILTERS) {
        withTransaction {
            getFilterDao().deleteGroupSync(DbFilterGroup.CURRENT_FILTER_GROUP_ID)
            insertCurrentFilterAndChildren(filters)
        }
    }

    private suspend fun insertCurrentFilterAndChildren(
        filters: List<Filter<*>>,
        parentId: Long? = null
    ) {
        filters.forEach { filter ->
            val id = getFilterDao().insertSingle(filter.toDbFilter(1, parentId))
            insertCurrentFilterAndChildren(filter.children, id)
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

    suspend fun deletePlaylist(id: Long) {
        getPlaylistDao().delete(DbPlaylist(id, "", ""))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        getPlaylistSongsDao().delete(DbPlaylistSongs(0, songId, playlistId))
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
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val currentFilterGroup = DbFilterGroup.currentFilterGroup
                        db.execSQL("INSERT INTO FilterGroup VALUES (${currentFilterGroup.id}, \"${currentFilterGroup.name}\")")
                    }
                })
                .addMigrations(Migration(1, 2) { db ->
                    db.execSQL("ALTER TABLE PlaylistSongs ADD COLUMN 'order' INTEGER NOT NULL DEFAULT 0")
                })
                .addMigrations(Migration(2, 3) { db ->
                    db.execSQL("ALTER TABLE Song ADD COLUMN downSamples TEXT NOT NULL DEFAULT \"\"")
                })
                .addMigrations(Migration(3, 4) { db ->
                    db.execSQL("ALTER TABLE Song RENAME COLUMN downSamples TO bars")
                })
                .addMigrations(Migration(4, 5) { db ->
                    db.execSQL("ALTER TABLE DbFilter ADD COLUMN parentFilter INTEGER DEFAULT NULL")
                })
                .build()
        }
    }
}