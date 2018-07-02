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
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers


@Database(entities = [Album::class, Artist::class, Playlist::class, QueueOrder::class, Song::class, DbFilter::class], version = 1)
abstract class LibraryDatabase : RoomDatabase() {

    protected abstract fun getAlbumDao(): AlbumDao
    protected abstract fun getArtistDao(): ArtistDao
    protected abstract fun getPlaylistDao(): PlaylistDao
    protected abstract fun getQueueOrderDao(): QueueOrderDao
    protected abstract fun getSongDao(): SongDao
    protected abstract fun getFilterDao(): FilterDao

    init {
        asyncCompletable {
            if (!getQueueOrderDao().hasQueue()) {
                getSongsForFilters()
                        .doOnNext { saveOrder(it) }
                        .subscribeOn(Schedulers.io())
                        .subscribe()
            }
        }.subscribe()
    }

    /**
     * Getters
     */

    fun getSongAtPosition(position: Int) = getSongDao().forPositionInQueue(position)

    fun getSongsInQueueOrder(): Flowable<PagedList<SongDisplay>> {
        val dataSourceFactory = getSongDao().inQueueOrder()
        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(100)
                .build()
        return RxPagedListBuilder(dataSourceFactory, pagedListConfig).buildFlowable(BackpressureStrategy.LATEST).subscribeOn(Schedulers.io())
    }

    fun getGenres(): Flowable<List<String>> = getSongDao().genreOrderedByGenre().subscribeOn(Schedulers.io())

    fun getArtists(): Flowable<List<ArtistDisplay>> = getArtistDao().orderedByName().subscribeOn(Schedulers.io())

    fun getAlbums(): Flowable<List<AlbumDisplay>> = getAlbumDao().orderedByName().subscribeOn(Schedulers.io())

    fun getFilters(): Flowable<List<Filter<*>>> = getFilterDao().all().map {
        val typedList = mutableListOf<Filter<*>>()
        it.forEach {
            typedList.add(Filter.toTypedFilter(it))
        }
        typedList as List<Filter<*>>
    }.subscribeOn(Schedulers.io())

    /**
     * Setters
     */

    fun addSongs(songs: List<Song>): Completable = asyncCompletable {
        getSongDao().insert(songs)
    }

    fun addArtists(artists: List<Artist>): Completable = asyncCompletable {
        getArtistDao().insert(artists)
    }

    fun addAlbums(albums: List<Album>): Completable = asyncCompletable {
        getAlbumDao().insert(albums)
    }

    fun addPlayLists(playlist: List<Playlist>): Completable = asyncCompletable {
        getPlaylistDao().insert(playlist)
    }

    fun addFilters(vararg filters: Filter<*>): Completable = asyncCompletable {
        getFilterDao().insert(filters.map { Filter.toDbFilter(it) })
    }

    fun clearFilters(): Completable = asyncCompletable {
        getFilterDao().deleteAll()
    }

    fun setFilters(songList: List<Filter<*>>): Completable = asyncCompletable {
        val filterDao = getFilterDao()
        filterDao.deleteAll()
        filterDao.insert(songList.map { Filter.toDbFilter(it) })
    }

    /**
     * Private methods
     */

    private fun getSongsForFilters(): Flowable<List<Long>> = getFilterDao().all().flatMap { dbFilters ->
        val typedFilterList = mutableListOf<Filter<*>>()
        dbFilters.forEach {
            typedFilterList.add(Filter.toTypedFilter(it))
        }
        var query = "SELECT id FROM song"

        query += if (typedFilterList.isNotEmpty()) {
            " WHERE"
        } else {
            ""
        }

        typedFilterList.forEachIndexed { index, filter ->
            query += when (filter) {
                is Filter.StringFilter -> " ${filter.clause} \"${filter.argument}\""
                is Filter.LongFilter -> " ${filter.clause} ${filter.argument}"
            }
            if (index < typedFilterList.size - 1) {
                query += " OR"
            }
        }
        getSongDao().forCurrentFilters(SimpleSQLiteQuery(query))
    }

    private fun saveOrder(it: List<Long>) {
        val queueOrder = mutableListOf<QueueOrder>()
        it.forEachIndexed { index, songId ->
            queueOrder.add(QueueOrder(index, songId))
        }
        asyncCompletable { getQueueOrderDao().setOrder(queueOrder)}.subscribe()
    }

    private fun asyncCompletable(action: () -> Unit): Completable = Completable.fromAction(action).subscribeOn(Schedulers.io())

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