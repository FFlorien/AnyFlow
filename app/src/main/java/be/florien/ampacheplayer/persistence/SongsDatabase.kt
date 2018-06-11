package be.florien.ampacheplayer.persistence

import android.content.Context
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.persistence.model.*
import be.florien.ampacheplayer.player.Filter
import io.reactivex.Observable
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

    fun getSongs(): Observable<List<Song>> = Observable.create<List<Song>> { LibraryDatabase.getInstance(context).getSongDao().getSong() }.subscribeOn(Schedulers.io())

    fun getGenres(): Observable<List<Song>> = Observable.create<List<Song>> { LibraryDatabase.getInstance(context).getSongDao().getSong() }.subscribeOn(Schedulers.io())

    fun getArtists(): Observable<List<Artist>> = Observable.create<List<Artist>> { LibraryDatabase.getInstance(context).getArtistDao().getArtist() }.subscribeOn(Schedulers.io())

    fun getAlbums(): Observable<List<Album>> = Observable.create<List<Album>> { LibraryDatabase.getInstance(context).getAlbumDao().getAlbum() }.subscribeOn(Schedulers.io())

    fun getQueueOrder(): Observable<List<QueueOrder>> = Observable.create<List<QueueOrder>> { LibraryDatabase.getInstance(context).getQueueOrderDao().getQueueOrder() }.subscribeOn(Schedulers.io())

    /**
     * Database getters : Filtered
     */

    fun getSongs(filters: List<Filter<*>> = emptyList()): Observable<List<Song>> = getSongs() //todo implement filter

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