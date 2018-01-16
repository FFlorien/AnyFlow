package be.florien.ampacheplayer.persistence

import be.florien.ampacheplayer.persistence.model.*
import be.florien.ampacheplayer.player.Filter
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import javax.inject.Inject

/**
 * Manager for the ampache data databaseManager-side
 */
class DatabaseManager
@Inject constructor(private val realmRead: Realm) {
    /**
     * Database getters : Unfiltered
     */

    fun getSongs(): RealmResults<Song> = realmRead.where(Song::class.java).sort("id").findAll()

    fun getGenres(): RealmResults<Song> = realmRead.where(Song::class.java).distinctValues("genre").findAll() //todo create genre RealmObject

    fun getArtists(): RealmResults<Artist> = realmRead.where(Artist::class.java).findAll()

    fun getAlbums(): RealmResults<Album> = realmRead.where(Album::class.java).findAll()

    /**
     * Database getters : Filtered
     */

    fun getSongs(filters: List<Filter<*>> = emptyList(), realmInstance: Realm = realmRead): RealmResults<Song> = realmInstance.let {
        val realmQuery = it.where(Song::class.java)
        var isFirstFilter = true
        for (filter in filters) {
            applyFilter(realmQuery, filter, isFirstFilter)
            isFirstFilter = false
        }
        realmQuery.sort("id")
        return realmQuery.findAll()
    }

    /**
     * Database setters
     */

    fun addSongs(songs: List<Song>): Unit =
            Realm.getDefaultInstance().let {
                it.executeTransaction { it.copyToRealmOrUpdate(songs) }
                it.close()
            }

    fun addArtists(artists: List<Artist>): Unit =
            Realm.getDefaultInstance().let {
                it.executeTransaction { realm -> realm.copyToRealmOrUpdate(artists) }
                it.close()
            }

    fun addAlbums(albums: List<Album>): Unit =
            Realm.getDefaultInstance().let {
                it.executeTransaction { realm -> realm.copyToRealmOrUpdate(albums) }
                it.close()
            }

    fun addTags(tags: List<Tag>): Unit =
            Realm.getDefaultInstance().let {
                it.executeTransaction { realm -> realm.copyToRealmOrUpdate(tags) }
                it.close()
            }

    fun addPlayLists(playlist: List<Playlist>): Unit =
            Realm.getDefaultInstance().let {
                it.executeTransaction { realm -> realm.copyToRealmOrUpdate(playlist) }
                it.close()
            }

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