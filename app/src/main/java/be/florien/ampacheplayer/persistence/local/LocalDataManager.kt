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
 * Manager for the ampache data databaseManager-side
 */
@UserScope
class LocalDataManager
@Inject constructor(val context: Context) {
    /**
     * Database getters : Unfiltered
     */

    fun getSongs(): Flowable<List<Song>> = LibraryDatabase.getInstance(context).getSongDao().getSong().subscribeOn(Schedulers.io())

    fun getSongsInQueueOrder(): Flowable<List<Song>> = LibraryDatabase.getInstance(context).getSongDao().getSongsInQueueOrder().subscribeOn(Schedulers.io())

//    fun getSongsForCurrentFilters(): Flowable<List<Song>> = Flowable.create<List<Song>>({
//        val filters = LibraryDatabase.getInstance(context).getFilterDao().getFilters()
//        var query = "SELECT * FROM song"
//
//        query += if (filters.isNotEmpty()) {
//            " WHERE"
//        } else {
//            ""
//        }
//
//        for (filter in filters) {
//            query += " ${filter.filterFunction}"
//        }
//
//        it.onNext(LibraryDatabase.getInstance(context).getSongDao().getSongsforCurrentFilters(SimpleSQLiteQuery(query)))
//    }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io())

    fun getGenres(): Flowable<List<Song>> = LibraryDatabase.getInstance(context).getSongDao().getSong().subscribeOn(Schedulers.io())

    fun getArtists(): Flowable<List<Artist>> = LibraryDatabase.getInstance(context).getArtistDao().getArtist().subscribeOn(Schedulers.io())

    fun getAlbums(): Flowable<List<Album>> = LibraryDatabase.getInstance(context).getAlbumDao().getAlbum().subscribeOn(Schedulers.io())

    /**
     * Database setters
     */

    fun addSongs(songs: List<Song>): Completable = Completable.fromAction {
        val queue = mutableListOf<QueueOrder>()

        songs.forEachIndexed { index, song ->
            queue.add(QueueOrder(index, song))
        }

        LibraryDatabase.getInstance(context).getSongDao().insert(songs)
        LibraryDatabase.getInstance(context).getQueueOrderDao().insert(queue)
    }.subscribeOn(Schedulers.io())

    fun addArtists(artists: List<Artist>): Completable = Completable.fromAction {
        LibraryDatabase.getInstance(context).getArtistDao().insert(artists)
    }.subscribeOn(Schedulers.io())

    fun addAlbums(albums: List<Album>): Completable = Completable.fromAction {
        LibraryDatabase.getInstance(context).getAlbumDao().insert(albums)
    }.subscribeOn(Schedulers.io())

    fun addTags(tags: List<Tag>): Completable = Completable.fromAction {
        LibraryDatabase.getInstance(context).getTagDao().insert(tags)
    }.subscribeOn(Schedulers.io())

    fun addPlayLists(playlist: List<Playlist>): Completable = Completable.fromAction {
        LibraryDatabase.getInstance(context).getPlaylistDao().insert(playlist)
    }.subscribeOn(Schedulers.io())

    fun setOrder(songList: List<QueueOrder>): Completable = Completable.fromAction {
        val queueOrderDao = LibraryDatabase.getInstance(context).getQueueOrderDao()
        queueOrderDao.deleteAll()
        queueOrderDao.insert(songList)
    }.subscribeOn(Schedulers.io())

//    fun setFilter(songList: List<Filter>): Completable = Completable.fromAction {
//        val filterDao = LibraryDatabase.getInstance(context).getFilterDao()
//        filterDao.deleteAll()
//        filterDao.insert(songList)
//    }.subscribeOn(Schedulers.io())
}