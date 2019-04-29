package be.florien.anyflow.persistence.local

import android.content.Context
import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.persistence.local.dao.*
import be.florien.anyflow.persistence.local.model.*
import be.florien.anyflow.player.Filter
import be.florien.anyflow.player.Order
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject


@Database(entities = [Album::class, Artist::class, Playlist::class, QueueOrder::class, Song::class, DbFilter::class, FilterGroup::class, DbOrder::class], version = 3)
abstract class LibraryDatabase : RoomDatabase() {

    protected abstract fun getAlbumDao(): AlbumDao
    protected abstract fun getArtistDao(): ArtistDao
    protected abstract fun getPlaylistDao(): PlaylistDao
    protected abstract fun getQueueOrderDao(): QueueOrderDao
    protected abstract fun getSongDao(): SongDao
    protected abstract fun getFilterDao(): FilterDao
    protected abstract fun getFilterGroupDao(): FilterGroupDao
    protected abstract fun getOrderDao(): OrderDao
    var randomOrderingSeed = 2
    var precisePosition = listOf<Order>()
    private val _changeUpdater: BehaviorSubject<Int> = BehaviorSubject.create()
    val changeUpdater = _changeUpdater.toFlowable(BackpressureStrategy.BUFFER)
            .share()
            .publish()
            .autoConnect()

    /**
     * Getters
     */

    fun getSongAtPosition(position: Int): Maybe<Song> = getSongDao()
            .forPositionInQueue(position)
            .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getSongAtPosition") }
            .subscribeOn(Schedulers.io())

    fun getPositionForSong(song: Song): Maybe<Int> = getSongDao()
            .findPositionInQueue(song.id)
            .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getPositionForSong") }
            .subscribeOn(Schedulers.io())

    fun getSongsInQueueOrder(): Flowable<PagedList<SongDisplay>> {
        val dataSourceFactory = getSongDao().displayInQueueOrder()
        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(100)
                .build()
        return RxPagedListBuilder(dataSourceFactory, pagedListConfig)
                .buildFlowable(BackpressureStrategy.LATEST)
                .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getSongsInQueueOrder") }
                .subscribeOn(Schedulers.io())
    }

    fun getSongsUrlInQueueOrder(): Flowable<List<String>> = getSongDao().urlInQueueOrder()

    fun <T> getGenres(mapping: (String) -> T): Flowable<PagedList<T>> {
        val dataSourceFactory = getSongDao().genreOrderByGenre().map(mapping)
        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(100)
                .build()
        return RxPagedListBuilder(dataSourceFactory, pagedListConfig)
                .buildFlowable(BackpressureStrategy.LATEST)
                .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getGenres") }.subscribeOn(Schedulers.io())
    }

    fun <T> getArtists(mapping: (ArtistDisplay) -> T): Flowable<PagedList<T>> {
        val dataSourceFactory = getArtistDao().orderByName().map(mapping)
        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(100)
                .build()
        return RxPagedListBuilder(dataSourceFactory, pagedListConfig)
                .buildFlowable(BackpressureStrategy.LATEST)
                .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getArtists") }.subscribeOn(Schedulers.io())
    }

    fun <T> getAlbums(mapping: (AlbumDisplay) -> T): Flowable<PagedList<T>> {
        val dataSourceFactory = getAlbumDao().orderByName().map(mapping)
        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(100)
                .build()
        return RxPagedListBuilder(dataSourceFactory, pagedListConfig)
                .buildFlowable(BackpressureStrategy.LATEST)
                .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getAlbums") }.subscribeOn(Schedulers.io())
    }

    fun getCurrentFilters(): Flowable<List<Filter<*>>> = getFilterDao()
            .currentFilters()
            .convertToFilters()
            .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getCurrentFilters") }.subscribeOn(Schedulers.io())

    fun getFiltersForGroup(group: FilterGroup): Flowable<List<Filter<*>>> = getFilterDao()
            .filterForGroup(group.id)
            .convertToFilters()
            .doOnError { this@LibraryDatabase.eLog(it, "Error while querying filtersForGroup") }.subscribeOn(Schedulers.io())

    fun getFilterGroups(): Flowable<List<FilterGroup>> = getFilterGroupDao()
            .allSavedFilterGroup()
            .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getFilterGroups") }.subscribeOn(Schedulers.io())

    private fun Flowable<List<DbFilter>>.convertToFilters(): Flowable<List<Filter<*>>> =
            map { filterList ->
                val typedList = mutableListOf<Filter<*>>()
                filterList.forEach {
                    typedList.add(Filter.toTypedFilter(it))
                }
                typedList as List<Filter<*>>
            }

    fun getOrder(): Flowable<List<DbOrder>> = getOrderDao().all().doOnError { this@LibraryDatabase.eLog(it, "Error while querying getOrder") }.subscribeOn(Schedulers.io())

    /**
     * Setters
     */

    fun addSongs(songs: List<Song>): Completable = asyncCompletable(CHANGE_SONGS) { getSongDao().insert(songs) }
            .doOnError { this@LibraryDatabase.eLog(it, "Error while addSongs") }

    fun addArtists(artists: List<Artist>): Completable = asyncCompletable(CHANGE_ARTISTS) { getArtistDao().insert(artists) }
            .doOnError { this@LibraryDatabase.eLog(it, "Error while addArtists") }

    fun addAlbums(albums: List<Album>): Completable = asyncCompletable(CHANGE_ALBUMS) { getAlbumDao().insert(albums) }
            .doOnError { this@LibraryDatabase.eLog(it, "Error while addAlbums") }

    fun addPlayLists(playlists: List<Playlist>): Completable = asyncCompletable(CHANGE_PLAYLISTS) { getPlaylistDao().insert(playlists) }
            .doOnError { this@LibraryDatabase.eLog(it, "Error while addPlayLists") }

    fun setCurrentFilters(filters: List<Filter<*>>): Completable =
            asyncCompletable(CHANGE_FILTER_GROUP) {
                getFilterDao().deleteCurrentFilters()
                val currentFilterGroup = FilterGroup(1, "Current Filters")
                val dbFilters = filters.map { it.toDbFilter(currentFilterGroup) }
                getFilterDao().insert(dbFilters)
            }
                    .doOnError { this@LibraryDatabase.eLog(it, "Error while setFilters") }

    fun createFilterGroup(filters: List<Filter<*>>, name: String):
            Completable =
            asyncCompletable(CHANGE_FILTER_GROUP) {
                val filterGroup = FilterGroup(0, name)
                val newId = getFilterGroupDao().insertSingle(filterGroup)
                val filterGroupUpdated = FilterGroup(newId, name)
                val filtersUpdated = filters.map { it.toDbFilter(filterGroupUpdated) }
                getFilterDao().insert(filtersUpdated)
            }
                    .doOnError { this@LibraryDatabase.eLog(it, "Error while setFilters") }

    fun setOrders(orders: List<DbOrder>): Completable = asyncCompletable(CHANGE_ORDER) { getOrderDao().replaceBy(orders) }
            .doOnError { this@LibraryDatabase.eLog(it, "Error while setOrders") }

    fun setOrdersSubject(orders: List<Long>): Completable = asyncCompletable(CHANGE_ORDER) {
        val dbOrders = orders.mapIndexed { index, order ->
            Order(index, order, Order.ASCENDING).toDbOrder()
        }
        getOrderDao().replaceBy(dbOrders)
    }.doOnError { this@LibraryDatabase.eLog(it, "Error while setOrdersSubject") }


    /**
     * Private methods
     */

    fun getPlaylist(): Flowable<List<Long>> =
            Flowable.merge(getPlaylistFromFilter(), getPlaylistFromOrder())

    private fun getPlaylistFromFilter(): Flowable<List<Long>> =
            getFilterDao()
                    .currentFilters()
                    .withLatestFrom(
                            getOrderDao().all(),
                            BiFunction { dbFilters: List<DbFilter>, dbOrders: List<DbOrder> ->
                                getQueryForSongs(dbFilters, dbOrders)
                            })
                    .flatMap {
                        getSongDao().forCurrentFilters(SimpleSQLiteQuery(it))
                    }
                    .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getPlaylistFromFilter") }

    private fun getPlaylistFromOrder(): Flowable<List<Long>> =
            getOrderDao()
                    .all()
                    .doOnNext { retrieveRandomness(it) }
                    .withLatestFrom(
                            getFilterDao().currentFilters(),
                            BiFunction { dbOrders: List<DbOrder>, dbFilters: List<DbFilter> ->
                                getQueryForSongs(dbFilters, dbOrders)
                            })
                    .flatMap {
                        getSongDao().forCurrentFilters(SimpleSQLiteQuery(it))
                    }
                    .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getPlaylistFromOrder") }

    private fun retrieveRandomness(dbOrders: List<DbOrder>) {
        val orderList = dbOrders.map { Order.toOrder(it) }
        randomOrderingSeed = orderList.firstOrNull { it.orderingType == Order.Ordering.RANDOM }?.argument
                ?: -1
        precisePosition = orderList.filter { it.orderingType == Order.Ordering.PRECISE_POSITION }
    }

    private fun getQueryForSongs(dbFilters: List<DbFilter>, dbOrders: List<DbOrder>): String {

        fun constructWhereStatement(): String {
            val typedFilterList = mutableListOf<Filter<*>>()
            dbFilters.forEach {
                typedFilterList.add(Filter.toTypedFilter(it))
            }

            var whereStatement = if (typedFilterList.isNotEmpty()) {
                " WHERE"
            } else {
                ""
            }

            typedFilterList.forEachIndexed { index, filter ->
                whereStatement += when (filter) {
                    is Filter.TitleIs,
                    is Filter.TitleContain,
                    is Filter.Search,
                    is Filter.GenreIs -> " ${filter.clause} \"${filter.argument}\""
                    is Filter.SongIs,
                    is Filter.ArtistIs,
                    is Filter.AlbumArtistIs,
                    is Filter.AlbumIs -> " ${filter.clause} ${filter.argument}"
                }
                if (index < typedFilterList.size - 1) {
                    whereStatement += " OR"
                }
            }

            return whereStatement
        }

        fun constructOrderStatement(): String {
            val orderList = mutableListOf<Order>()
            dbOrders.forEach {
                orderList.add(Order.toOrder(it))
            }
            val filteredOrderedList = orderList.filter { it.orderingType != Order.Ordering.PRECISE_POSITION }

            val isSorted = filteredOrderedList.isNotEmpty() && filteredOrderedList.all { it.orderingType != Order.Ordering.RANDOM }

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

        return "SELECT id FROM song" + constructWhereStatement() + constructOrderStatement()
    }

    fun saveOrder(it: List<Long>) {
        val queueOrder = mutableListOf<QueueOrder>()
        it.forEachIndexed { index, songId ->
            queueOrder.add(QueueOrder(index, songId))
        }
        asyncCompletable(CHANGE_QUEUE) { getQueueOrderDao().setOrder(queueOrder) }.doOnError { this@LibraryDatabase.eLog(it, "Error while saveOrder") }.subscribe()
    }

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