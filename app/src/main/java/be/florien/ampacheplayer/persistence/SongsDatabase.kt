package be.florien.ampacheplayer.persistence

import android.content.Context
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.persistence.model.*
import be.florien.ampacheplayer.player.Filter
import io.reactivex.BackpressureStrategy
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

    fun getSongs(): Flowable<List<Song>> = Flowable.create<List<Song>>({ it.onNext(LibraryDatabase.getInstance(context).getSongDao().getSong()) }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io())

    fun getGenres(): Flowable<List<Song>> = Flowable.create<List<Song>>({ it.onNext(LibraryDatabase.getInstance(context).getSongDao().getSong()) }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io())

    fun getArtists(): Flowable<List<Artist>> = Flowable.create<List<Artist>>({ it.onNext(LibraryDatabase.getInstance(context).getArtistDao().getArtist()) }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io())

    fun getAlbums(): Flowable<List<Album>> = Flowable.create<List<Album>>({ it.onNext(LibraryDatabase.getInstance(context).getAlbumDao().getAlbum()) }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io())

    fun getQueueOrder(): Flowable<List<QueueOrder>> = Flowable.create<List<QueueOrder>>({ it.onNext(LibraryDatabase.getInstance(context).getQueueOrderDao().getQueueOrder()) }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io())

    /**
     * Database getters : Filtered
     */

    fun getSongs(filters: List<Filter<*>> = emptyList()): Flowable<List<Song>> = getSongs() //todo implement filter

    /**
     * Database setters
     */

    fun addSongs(songs: List<Song>): Unit = LibraryDatabase.getInstance(context).getSongDao().insert(songs)

    fun addArtists(artists: List<Artist>): Unit = LibraryDatabase.getInstance(context).getArtistDao().insert(artists)

    fun addAlbums(albums: List<Album>): Unit = LibraryDatabase.getInstance(context).getAlbumDao().insert(albums)

    fun addTags(tags: List<Tag>): Unit = LibraryDatabase.getInstance(context).getTagDao().insert(tags)

    fun addPlayLists(playlist: List<Playlist>): Unit = LibraryDatabase.getInstance(context).getPlaylistDao().insert(playlist)

    fun setOrder(songList: List<QueueOrder>): Unit = LibraryDatabase.getInstance(context).getQueueOrderDao().insert(songList)

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