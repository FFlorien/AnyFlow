package be.florien.ampacheplayer.persistence.local

import android.arch.persistence.db.SimpleSQLiteQuery
import android.content.Context
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.persistence.local.model.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * Manager for the ampache data database-side
 */
@UserScope
class LocalDataManager
@Inject constructor(private val context: Context) {
    private val libraryDatabase: LibraryDatabase = LibraryDatabase.getInstance(context)

    init {
        getSongsForFilters().distinct().doOnNext {
            saveOrder(it)
        }
    }

    private fun saveOrder(it: List<Song>) {
        val queueOrder = mutableListOf<QueueOrder>()
        it.forEachIndexed { index, song ->
            queueOrder.add(QueueOrder(index, song))
        }
        setOrder(queueOrder)
    }

    /**
     * Database getters : Unfiltered
     */

    fun getSongs(): Flowable<List<Song>> = libraryDatabase.getSongDao().getSongs().subscribeOn(Schedulers.io())

    fun getSongsInQueueOrder(): Flowable<List<Song>> = libraryDatabase.getSongDao().getSongsInQueueOrder().subscribeOn(Schedulers.io())

    private fun getSongsForFilters(): Flowable<List<Song>> = Flowable.create<List<Song>>({
        val filters = LibraryDatabase.getInstance(context).getFilterDao().getFiltersSync().map { Filter.getTypedFilter(it) }
        var query = "SELECT * FROM song"

        query += if (filters.isNotEmpty()) {
            " WHERE"
        } else {
            ""
        }

        filters.forEachIndexed { index, filter ->
            query += " ${filter.clause}"
            if (index < filters.size - 1) {
                query += " AND"
            }
        }

        it.onNext(LibraryDatabase.getInstance(context).getSongDao().getSongsforCurrentFilters(SimpleSQLiteQuery(query)))
    }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io())

    fun getGenres(): Flowable<List<Song>> = libraryDatabase.getSongDao().getSongs().subscribeOn(Schedulers.io())

    fun getArtists(): Flowable<List<Artist>> = libraryDatabase.getArtistDao().getArtist().subscribeOn(Schedulers.io())

    fun getAlbums(): Flowable<List<Album>> = libraryDatabase.getAlbumDao().getAlbum().subscribeOn(Schedulers.io())

    fun getFilters(): Flowable<List<Filter>> = libraryDatabase.getFilterDao().getFilters().map {
        val typedList = mutableListOf<Filter>()
        it.forEach {
            Filter.getTypedFilter(it)
        }
        typedList as List<Filter>
    }.subscribeOn(Schedulers.io())

    /**
     * Database setters
     */

    fun addSongs(songs: List<Song>): Completable = asyncCompletable {
        libraryDatabase.getSongDao().insert(songs)
    }

    fun addArtists(artists: List<Artist>): Completable = asyncCompletable {
        libraryDatabase.getArtistDao().insert(artists)
    }

    fun addAlbums(albums: List<Album>): Completable = asyncCompletable {
        libraryDatabase.getAlbumDao().insert(albums)
    }

    fun addTags(tags: List<Tag>): Completable = asyncCompletable {
        libraryDatabase.getTagDao().insert(tags)
    }

    fun addPlayLists(playlist: List<Playlist>): Completable = asyncCompletable {
        libraryDatabase.getPlaylistDao().insert(playlist)
    }

    private fun setOrder(orderList: List<QueueOrder>): Completable = asyncCompletable {
        val queueOrderDao = libraryDatabase.getQueueOrderDao()
        queueOrderDao.deleteAll()
        queueOrderDao.insert(orderList)
    }

    fun clearFilters(): Completable = asyncCompletable {
        libraryDatabase.getFilterDao().deleteAll()
    }

    fun addFilters(filters: List<Filter>): Completable = asyncCompletable {
        libraryDatabase.getFilterDao().insert(filters.map { Filter.toDbFilter(it) })
    }

    fun setFilters(songList: List<Filter>): Completable = asyncCompletable {
        val filterDao = libraryDatabase.getFilterDao()
        filterDao.deleteAll()
        filterDao.insert(songList.map { Filter.toDbFilter(it) })
    }

    private fun asyncCompletable(action: () -> Unit): Completable = Completable.fromAction(action).subscribeOn(Schedulers.io())
}