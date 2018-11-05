package be.florien.anyflow.persistence.local

import android.arch.paging.PagedList
import android.arch.paging.RxPagedListBuilder
import android.arch.persistence.db.SimpleSQLiteQuery
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.persistence.local.dao.*
import be.florien.anyflow.persistence.local.model.*
import be.florien.anyflow.player.Filter
import be.florien.anyflow.player.Order
import be.florien.anyflow.player.Subject
import io.reactivex.*
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*


@Database(entities = [Album::class, Artist::class, Playlist::class, QueueOrder::class, Song::class, DbFilter::class, DbOrder::class], version = 1)
abstract class LibraryDatabase : RoomDatabase() {

    protected abstract fun getAlbumDao(): AlbumDao
    protected abstract fun getArtistDao(): ArtistDao
    protected abstract fun getPlaylistDao(): PlaylistDao
    protected abstract fun getQueueOrderDao(): QueueOrderDao
    protected abstract fun getSongDao(): SongDao
    protected abstract fun getFilterDao(): FilterDao
    protected abstract fun getOrderDao(): OrderDao
    private var randomOrderingSeed = 2
    private val _changeUpdater: BehaviorSubject<Int> = BehaviorSubject.create()
    val changeUpdater = _changeUpdater.toFlowable(BackpressureStrategy.BUFFER)
            .share()
            .publish()
            .autoConnect()

    init {
        getPlaylist()
                .doOnNext {
                    val listToSave = if (randomOrderingSeed >= 0) {
                        it.shuffled(Random(randomOrderingSeed.toLong()))
                    } else {
                        it
                    }
                    saveOrder(listToSave)
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    /**
     * Getters
     */

    fun getSongAtPosition(position: Int): Maybe<Song> = getSongDao().forPositionInQueue(position).doOnError { this@LibraryDatabase.eLog(it, "Error while querying getSongAtPosition") }.subscribeOn(Schedulers.io())

    fun getPositionForSong(song: Song): Single<Int> = getSongDao().findPositionInQueue(song.id).doOnError { this@LibraryDatabase.eLog(it, "Error while querying getPositionForSong") }.subscribeOn(Schedulers.io())

    fun getSongsInQueueOrder(): Flowable<PagedList<SongDisplay>> {
        val dataSourceFactory = getSongDao().inQueueOrder()
        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(100)
                .build()
        return RxPagedListBuilder(dataSourceFactory, pagedListConfig).buildFlowable(BackpressureStrategy.LATEST).doOnError { this@LibraryDatabase.eLog(it, "Error while querying getSongsInQueueOrder") }.subscribeOn(Schedulers.io())
    }

    fun getGenres(): Flowable<List<String>> = getSongDao().genreOrderByGenre().doOnError { this@LibraryDatabase.eLog(it, "Error while querying getGenres") }.subscribeOn(Schedulers.io())

    fun getArtists(): Flowable<List<ArtistDisplay>> = getArtistDao().orderByName().doOnError { this@LibraryDatabase.eLog(it, "Error while querying getArtists") }.subscribeOn(Schedulers.io())

    fun getAlbums(): Flowable<List<AlbumDisplay>> = getAlbumDao().orderByName().doOnError { this@LibraryDatabase.eLog(it, "Error while querying getAlbums") }.subscribeOn(Schedulers.io())

    fun getFilters(): Flowable<List<Filter<*>>> = getFilterDao().all().map { filterList ->
        val typedList = mutableListOf<Filter<*>>()
        filterList.forEach {
            typedList.add(Filter.toTypedFilter(it))
        }
        typedList as List<Filter<*>>
    }.doOnError { this@LibraryDatabase.eLog(it, "Error while querying getFilters") }.subscribeOn(Schedulers.io())

    fun getOrder(): Flowable<List<DbOrder>> = getOrderDao().all().doOnError { this@LibraryDatabase.eLog(it, "Error while querying getOrder") }.subscribeOn(Schedulers.io())

    /**
     * Setters
     */

    fun addSongs(songs: List<Song>): Completable = asyncCompletable(CHANGE_SONGS) { getSongDao().insert(songs) }.doOnError { this@LibraryDatabase.eLog(it, "Error while addSongs") }

    fun addArtists(artists: List<Artist>): Completable = asyncCompletable(CHANGE_ARTISTS) { getArtistDao().insert(artists) }.doOnError { this@LibraryDatabase.eLog(it, "Error while addArtists") }

    fun addAlbums(albums: List<Album>): Completable = asyncCompletable(CHANGE_ALBUMS) { getAlbumDao().insert(albums) }.doOnError { this@LibraryDatabase.eLog(it, "Error while addAlbums") }

    fun addPlayLists(playlists: List<Playlist>): Completable = asyncCompletable(CHANGE_PLAYLISTS) { getPlaylistDao().insert(playlists) }.doOnError { this@LibraryDatabase.eLog(it, "Error while addPlayLists") }

    fun addFilters(vararg filters: DbFilter): Completable = asyncCompletable(CHANGE_FILTERS) { getFilterDao().insert(filters.toList()) }.doOnError { this@LibraryDatabase.eLog(it, "Error while addFilters") }

    fun deleteFilter(filter: DbFilter): Completable = asyncCompletable(CHANGE_FILTERS) { getFilterDao().delete(filter) }.doOnError { this@LibraryDatabase.eLog(it, "Error while deleteFilter") }

    fun clearFilters(): Completable = asyncCompletable(CHANGE_FILTERS) { getFilterDao().deleteAll() }.doOnError { this@LibraryDatabase.eLog(it, "Error while clearFilters") }

    fun setFilters(filters: List<DbFilter>): Completable = asyncCompletable(CHANGE_FILTERS) { getFilterDao().replaceBy(filters) }.doOnError { this@LibraryDatabase.eLog(it, "Error while setFilters") }

    fun setOrders(orders: List<DbOrder>): Completable = asyncCompletable(CHANGE_ORDER) { getOrderDao().replaceBy(orders) }.doOnError { this@LibraryDatabase.eLog(it, "Error while setOrders") }

    fun setOrdersSubject(orders: List<Subject>): Completable = asyncCompletable(CHANGE_ORDER) {
        val dbOrders = orders.mapIndexed { index, order ->
            Order(index, order, Order.ASCENDING).toDbOrder()
        }
        getOrderDao().replaceBy(dbOrders)
    }.doOnError { this@LibraryDatabase.eLog(it, "Error while setOrdersSubject") }


    /**
     * Private methods
     */

    private fun getPlaylist(): Flowable<List<Long>> =
            Flowable.merge(getPlaylistFromFilter(), getPlaylistFromOrder())

    private fun getPlaylistFromFilter(): Flowable<List<Long>> =
            getFilterDao()
                    .all()
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
                            getFilterDao().all(),
                            BiFunction { dbOrders: List<DbOrder>, dbFilters: List<DbFilter> ->
                                getQueryForSongs(dbFilters, dbOrders)
                            })
                    .flatMap {
                        getSongDao().forCurrentFilters(SimpleSQLiteQuery(it))
                    }
                    .doOnError { this@LibraryDatabase.eLog(it, "Error while querying getPlaylistFromOrder") }

    private fun retrieveRandomness(dbOrders: List<DbOrder>) {
        val orderList = dbOrders.map { Order.toOrder(it) }
        randomOrderingSeed = orderList.firstOrNull { it.isRandom }?.ordering ?: -1
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

            val isSorted = orderList.isNotEmpty() && orderList.all { !it.isRandom }

            var orderStatement = if (isSorted) {
                " ORDER BY"
            } else {
                ""
            }

            if (isSorted) {
                orderList.forEachIndexed { index, order ->
                    orderStatement += when (order.subject) {
                        Subject.ALL -> " song.id"
                        Subject.ARTIST -> " song.artistName"
                        Subject.ALBUM_ARTIST -> " song.albumArtistName"
                        Subject.ALBUM -> " song.albumName"
                        Subject.YEAR -> " song.year"
                        Subject.GENRE -> " song.genre"
                        Subject.TRACK -> " song.track"
                        Subject.TITLE -> " song.title"
                    }
                    orderStatement += when (order.ordering) {
                        Order.ASCENDING -> " ASC"
                        Order.DESCENDING -> " DESC"
                        else -> ""
                    }
                    if (index < orderList.size - 1) {
                        orderStatement += ","
                    }
                }
            }

            return orderStatement
        }

        return "SELECT id FROM song" + constructWhereStatement() + constructOrderStatement()
    }

    private fun saveOrder(it: List<Long>) {
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

        @Volatile
        private var instance: LibraryDatabase? = null
        private const val DB_NAME = "ampacheDatabase.db" //todo before release, more generic name

        fun getInstance(context: Context): LibraryDatabase {
            if (instance == null) {
                instance = create(context)
            }
            return instance!!
        }

        @Synchronized
        private fun create(context: Context): LibraryDatabase {
            return Room.databaseBuilder(context, LibraryDatabase::class.java, DB_NAME).build()
        }

    }
}