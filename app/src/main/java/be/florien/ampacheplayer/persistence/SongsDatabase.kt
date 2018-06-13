package be.florien.ampacheplayer.persistence

import android.content.Context
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.persistence.model.*
import be.florien.ampacheplayer.player.Filter
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.realm.RealmQuery
import javax.inject.Inject

/**
 * Manager for the ampache data databaseManager-side
 */
@UserScope
class SongsDatabase
@Inject constructor(val context: Context) {
    /**
     * Database getters : Unfiltered
     */

    fun getSongs(): Flowable<List<Song>> = LibraryDatabase.getInstance(context).getSongDao().getSong().subscribeOn(Schedulers.io())

    fun getSongsInQueueOrder(): Flowable<List<Song>> = LibraryDatabase.getInstance(context).getQueueOrderDao().getSongsInQueueOrder().subscribeOn(Schedulers.io())

    fun getGenres(): Flowable<List<Song>> = LibraryDatabase.getInstance(context).getSongDao().getSong().subscribeOn(Schedulers.io())

    fun getArtists(): Flowable<List<Artist>> = LibraryDatabase.getInstance(context).getArtistDao().getArtist().subscribeOn(Schedulers.io())

    fun getAlbums(): Flowable<List<Album>> = LibraryDatabase.getInstance(context).getAlbumDao().getAlbum().subscribeOn(Schedulers.io())

    fun getQueueOrder(): Flowable<List<QueueOrder>> = LibraryDatabase.getInstance(context).getQueueOrderDao().getQueueOrder().subscribeOn(Schedulers.io())

    /**
     * Database getters : Filtered
     */

    fun getSongs(filters: List<Filter<*>> = emptyList()): Flowable<List<Song>> = getSongs() //todo implement filter

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
        LibraryDatabase.getInstance(context).getQueueOrderDao().insert(songList)
    }.subscribeOn(Schedulers.io())

    /**
     * Private methods
     */

    private fun applyFilter(realmQuery: RealmQuery<Song>, filter: Filter<*>, isFirst: Boolean) {
        if (!isFirst) {
            realmQuery.or()
        }

        realmQuery.beginGroup()
        filter.apply {
            applyFilter(realmQuery)
            var isFirstSubFilter = true
            for (subFilter in subFilter) {
                applyFilter(realmQuery, subFilter, isFirstSubFilter)
                isFirstSubFilter = false
            }
        }
        realmQuery.endGroup()
    }
}