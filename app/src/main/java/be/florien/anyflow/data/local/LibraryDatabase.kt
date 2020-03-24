package be.florien.anyflow.data.local

import android.content.Context
import androidx.paging.DataSource
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import be.florien.anyflow.data.local.dao.*
import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.extension.eLog
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject


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
    private val _changeUpdater: BehaviorSubject<Int> = BehaviorSubject.create()
    val changeUpdater = _changeUpdater.toFlowable(BackpressureStrategy.BUFFER)
            .share()
            .publish()
            .autoConnect()

    /**
     * Getters
     */

    fun getSongAtPosition(position: Int): Maybe<DbSong> = getSongDao()
            .forPositionInQueue(position)
            .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getSongAtPosition") }
            .subscribeOn(Schedulers.io())

    fun getPositionForSong(song: DbSongDisplay): Maybe<Int> = getSongDao()
            .findPositionInQueue(song.id)
            .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getPositionForSong") }
            .subscribeOn(Schedulers.io())

    fun getSongsInQueueOrder() = getSongDao().displayInQueueOrder()

    fun getSongsFromQuery(query: String) = getSongDao().forCurrentFilters(SimpleSQLiteQuery(query))

    fun getGenres() = getSongDao().genreOrderByGenre()

    fun getArtists() = getArtistDao().orderByName()

    fun getAlbums(): DataSource.Factory<Int, DbAlbumDisplay> = getAlbumDao().orderByName()

    fun getCurrentFilters(): Flowable<List<DbFilter>> = getFilterDao()
            .currentFilters()
            .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getCurrentFilters") }.subscribeOn(Schedulers.io())

    fun getFilterGroups(): Flowable<List<DbFilterGroup>> = getFilterGroupDao()
            .allSavedFilterGroup()
            .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getFilterGroups") }.subscribeOn(Schedulers.io())

    fun setSavedGroupAsCurrentFilters(filterGroup: DbFilterGroup): Completable =
            getFilterDao()
                    .filterForGroupAsync(filterGroup.id)
                    .flatMapCompletable {
                        setCurrentFilters(it)
                    }

    fun getOrders(): Flowable<List<DbOrder>> = getOrderDao().all().doOnError { this@LibraryDatabase.eLog(it, "Error while querying getOrder") }.subscribeOn(Schedulers.io())

    /**
     * Setters
     */

    fun addSongs(songs: List<DbSong>): Completable = asyncCompletable(CHANGE_SONGS) { getSongDao().insert(songs) }
            .doOnError { this@LibraryDatabase.eLog(it, "Error while addSongs") }

    fun addArtists(artists: List<DbArtist>): Completable = asyncCompletable(CHANGE_ARTISTS) { getArtistDao().insert(artists) }
            .doOnError { this@LibraryDatabase.eLog(it, "Error while addArtists") }

    fun addAlbums(albums: List<DbAlbum>): Completable = asyncCompletable(CHANGE_ALBUMS) { getAlbumDao().insert(albums) }
            .doOnError { this@LibraryDatabase.eLog(it, "Error while addAlbums") }

    fun addPlayLists(playlists: List<DbPlaylist>): Completable = asyncCompletable(CHANGE_PLAYLISTS) { getPlaylistDao().insert(playlists) }
            .doOnError { this@LibraryDatabase.eLog(it, "Error while addPlayLists") }

    fun setCurrentFilters(filters: List<DbFilter>): Completable =
            asyncCompletable(CHANGE_FILTER_GROUP) {
                val currentFilterGroup = DbFilterGroup(1, "Current Filters")
                getFilterGroupDao().insertSingle(currentFilterGroup)
                getFilterDao().updateGroup(currentFilterGroup, filters.map { it.copy(filterGroup = 1) })
            }
                    .doOnError { this@LibraryDatabase.eLog(it, "Error while setFilters") }

    fun correctAlbumArtist(songs: List<DbSong>): Completable =
            asyncCompletable(CHANGE_SONGS) {
                songs.distinctBy { it.albumId }
                        .forEach { getAlbumDao().updateAlbumArtist(it.albumId, it.albumArtistId, it.albumArtistName) }
            }
                    .doOnError { this@LibraryDatabase.eLog(it, "Error while correctAlbumArtist") }

    fun createFilterGroup(filters: List<DbFilter>, name: String): Completable =
            asyncCompletable(CHANGE_FILTER_GROUP) {
                val filterGroup = DbFilterGroup(0, name)
                val newId = getFilterGroupDao().insertSingle(filterGroup)
                val filtersUpdated = filters.map { it.copy(filterGroup = newId) }
                getFilterDao().insert(filtersUpdated)
            }.doOnError { this@LibraryDatabase.eLog(it, "Error while setFilters") }

    fun filterForGroupSync(id: Long) = getFilterDao().filterForGroupSync(id)
    fun artForFilters(whereStatement: String) = getSongDao().artForFilters(SimpleSQLiteQuery(getQueryForFiltersArt(whereStatement)))

    fun setOrders(orders: List<DbOrder>): Completable = asyncCompletable(CHANGE_ORDER) { getOrderDao().replaceBy(orders) }
            .doOnError { this@LibraryDatabase.eLog(it, "Error while setOrders") }

    fun saveQueueOrder(it: List<Long>) {
        val queueOrder = mutableListOf<DbQueueOrder>()
        it.forEachIndexed { index, songId ->
            queueOrder.add(DbQueueOrder(index, songId))
        }
        asyncCompletable(CHANGE_QUEUE) { getQueueOrderDao().setOrder(queueOrder) }.doOnError { this@LibraryDatabase.eLog(it, "Error while saveOrder") }.subscribe()
    }

    /**
     * Private methods
     */

    private fun getQueryForFiltersArt(whereStatement: String) = "SELECT DISTINCT art FROM song$whereStatement"

    private fun asyncCompletable(changeSubject: Int, action: () -> Unit): Completable {
        _changeUpdater.onNext(changeSubject)
        return Completable.defer { Completable.fromAction(action) }.subscribeOn(Schedulers.io())
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