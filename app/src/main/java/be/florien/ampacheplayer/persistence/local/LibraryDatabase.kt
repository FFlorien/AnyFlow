package be.florien.ampacheplayer.persistence.local

import android.arch.paging.PagedList
import android.arch.paging.RxPagedListBuilder
import android.arch.persistence.db.SimpleSQLiteQuery
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import be.florien.ampacheplayer.persistence.local.dao.*
import be.florien.ampacheplayer.persistence.local.model.*
import be.florien.ampacheplayer.player.Filter
import be.florien.ampacheplayer.player.Order
import be.florien.ampacheplayer.player.Ordering
import be.florien.ampacheplayer.player.Subject
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers


@Database(entities = [Album::class, Artist::class, Playlist::class, QueueOrder::class, Song::class, DbFilter::class, DbOrder::class], version = 1)
abstract class LibraryDatabase : RoomDatabase() {

    protected abstract fun getAlbumDao(): AlbumDao
    protected abstract fun getArtistDao(): ArtistDao
    protected abstract fun getPlaylistDao(): PlaylistDao
    protected abstract fun getQueueOrderDao(): QueueOrderDao
    protected abstract fun getSongDao(): SongDao
    protected abstract fun getFilterDao(): FilterDao
    protected abstract fun getOrderDao(): OrderDao
    private var orderingIsRandom: Boolean = false

    init {
        getPlaylist()
                .doOnNext {
                    val listToSave = if (orderingIsRandom) {
                        it.shuffled()
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

    fun getSongAtPosition(position: Int): Maybe<Song> = getSongDao().forPositionInQueue(position).subscribeOn(Schedulers.io())

    fun getPositionForSong(song: Song): Maybe<Int> = getSongDao().findPositionInQueue(song.id).subscribeOn(Schedulers.io())

    fun getSongsInQueueOrder(): Flowable<PagedList<SongDisplay>> {
        val dataSourceFactory = getSongDao().inQueueOrder()
        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(100)
                .build()
        return RxPagedListBuilder(dataSourceFactory, pagedListConfig).buildFlowable(BackpressureStrategy.LATEST).subscribeOn(Schedulers.io())
    }

    fun getGenres(): Flowable<List<String>> = getSongDao().genreOrderByGenre().subscribeOn(Schedulers.io())

    fun getArtists(): Flowable<List<ArtistDisplay>> = getArtistDao().orderByName().subscribeOn(Schedulers.io())

    fun getAlbums(): Flowable<List<AlbumDisplay>> = getAlbumDao().orderByName().subscribeOn(Schedulers.io())

    fun getFilters(): Flowable<List<Filter<*>>> = getFilterDao().all().map {
        val typedList = mutableListOf<Filter<*>>()
        it.forEach {
            typedList.add(Filter.toTypedFilter(it))
        }
        typedList as List<Filter<*>>
    }.subscribeOn(Schedulers.io())

    fun getOrder() = getOrderDao().all()

    /**
     * Setters
     */

    fun addSongs(songs: List<Song>): Completable = asyncCompletable { getSongDao().insert(songs) }

    fun addArtists(artists: List<Artist>): Completable = asyncCompletable { getArtistDao().insert(artists) }

    fun addAlbums(albums: List<Album>): Completable = asyncCompletable { getAlbumDao().insert(albums) }

    fun addPlayLists(playlists: List<Playlist>): Completable = asyncCompletable { getPlaylistDao().insert(playlists) }

    fun addFilters(vararg filters: DbFilter): Completable = asyncCompletable { getFilterDao().insert(filters.toList()) }

    fun clearFilters(): Completable = asyncCompletable { getFilterDao().deleteAll() }

    fun setFilters(filters: List<DbFilter>): Completable = asyncCompletable { getFilterDao().replaceBy(filters) }

    fun setOrders(orders: List<DbOrder>): Completable = asyncCompletable { getOrderDao().replaceBy(orders) }

    fun setOrdersSubject(orders: List<Subject>): Completable = asyncCompletable {
        val dbOrders = orders.mapIndexed { index, order ->
            Order(index, order, Ordering.ASCENDING).toDbOrder()
        }
        getOrderDao().replaceBy(dbOrders)
    }


    /**
     * Private methods
     */

    private fun getPlaylist(): Flowable<List<Long>> =
            Flowable.merge(getPlaylistFromFilter(), getPlaylistFromOrder())

    private fun getPlaylistFromFilter(): Flowable<List<Long>> = getFilterDao().all()
            .withLatestFrom(
                    getOrderDao().all(),
                    BiFunction { dbFilters: List<DbFilter>, dbOrders: List<DbOrder> ->
                        getQueryForSongs(dbFilters, dbOrders)
                    })
            .flatMap {
                getSongDao().forCurrentFilters(SimpleSQLiteQuery(it))
            }

    private fun getPlaylistFromOrder(): Flowable<List<Long>> = getOrderDao().all()
            .withLatestFrom(
                    getFilterDao().all(),
                    BiFunction { dbOrders: List<DbOrder>, dbFilters: List<DbFilter> ->
                        getQueryForSongs(dbFilters, dbOrders)
                    })
            .flatMap {
                getSongDao().forCurrentFilters(SimpleSQLiteQuery(it))
            }

    private fun getQueryForSongs(dbFilters: List<DbFilter>, dbOrders: List<DbOrder>): String {
        val typedFilterList = mutableListOf<Filter<*>>()
        val orderList = mutableListOf<Order>()
        dbFilters.forEach {
            typedFilterList.add(Filter.toTypedFilter(it))
        }
        dbOrders.forEach {
            orderList.add(Order.toOrder(it))
        }
        var query = "SELECT id FROM song"

        query += if (typedFilterList.isNotEmpty()) {
            " WHERE"
        } else {
            ""
        }

        typedFilterList.forEachIndexed { index, filter ->
            query += when (filter) {
                is Filter.TitleIs,
                is Filter.TitleContain,
                is Filter.GenreIs -> " ${filter.clause} \"${filter.argument}\""
                is Filter.SongIs,
                is Filter.ArtistIs,
                is Filter.AlbumArtistIs,
                is Filter.AlbumIs -> " ${filter.clause} ${filter.argument}"
            }
            if (index < typedFilterList.size - 1) {
                query += " OR"
            }
        }

        query += if (orderList.isNotEmpty() && orderList.all { it.ordering != Ordering.RANDOM }) {
            " ORDER BY"
        } else {
            ""
        }

        orderingIsRandom = orderList.all { it.ordering == Ordering.RANDOM }

        if (!orderingIsRandom) {
            orderList.forEachIndexed { index, order ->
                query += when (order.subject) {
                    Subject.ALL -> " song.id"
                    Subject.ARTIST -> " song.artistName"
                    Subject.ALBUM_ARTIST -> " song.albumArtistName"
                    Subject.ALBUM -> " song.albumName"
                    Subject.YEAR -> " song.year"
                    Subject.GENRE -> " song.genre"
                    Subject.TRACK -> " song.track"
                    Subject.TITLE -> " song.title"
                }
                query += when (order.ordering) {
                    Ordering.ASCENDING -> " ASC"
                    Ordering.DESCENDING -> " DESC"
                    Ordering.RANDOM -> ""
                }
                if (index < orderList.size - 1) {
                    query += ","
                }
            }
        }
        return query
    }

    private fun saveOrder(it: List<Long>) {
        val queueOrder = mutableListOf<QueueOrder>()
        it.forEachIndexed { index, songId ->
            queueOrder.add(QueueOrder(index, songId))
        }
        asyncCompletable { getQueueOrderDao().setOrder(queueOrder) }.subscribe()
    }

    private fun asyncCompletable(action: () -> Unit): Completable = Completable.defer { Completable.fromAction(action) }.subscribeOn(Schedulers.io())

    companion object {
        @Volatile
        private var instance: LibraryDatabase? = null
        private const val DB_NAME = "ampacheDatabase.db"

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