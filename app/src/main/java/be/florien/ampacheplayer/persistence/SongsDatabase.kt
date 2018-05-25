package be.florien.ampacheplayer.persistence

import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.extension.realmInstance
import be.florien.ampacheplayer.persistence.model.*
import be.florien.ampacheplayer.player.Filter
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import javax.inject.Inject

/**
 * Manager for the ampache data databaseManager-side
 */
@UserScope
class SongsDatabase
@Inject constructor() {
    /**
     * Database getters : Unfiltered
     */

    fun getSongs(): RealmResults<Song> = Thread.currentThread().realmInstance.where(Song::class.java).sort("id").findAll()

    fun getGenres(): RealmResults<Song> = Thread.currentThread().realmInstance.where(Song::class.java).distinctValues("genre").findAll() //todo create genre RealmObject

    fun getArtists(): RealmResults<Artist> = Thread.currentThread().realmInstance.where(Artist::class.java).findAll()

    fun getAlbums(): RealmResults<Album> = Thread.currentThread().realmInstance.where(Album::class.java).findAll()

    /**
     * Database getters : Filtered
     */

    fun getSongs(filters: List<Filter<*>> = emptyList()): RealmResults<Song> = Thread.currentThread().realmInstance.let {
        val realmQuery = it.where(Song::class.java)
        var isFirstFilter = true
        for (filter in filters) {
            applyFilter(realmQuery, filter, isFirstFilter)
            isFirstFilter = false
        }
        realmQuery.sort("id")
        setOrderRandom(realmQuery.findAll())

        return it.where(Song::class.java).sort("queueOrder.order").findAll()
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

    fun setOrderRandom(songList: RealmResults<Song>) {

        if (Thread.currentThread().realmInstance.where(QueueOrder::class.java).count() > 0) {
            Thread.currentThread().realmInstance.delete(QueueOrder::class.java)
        }

        val orders = mutableListOf<Int>()
        for (order in 0 until songList.size) {
            orders[order] = order
        }

        orders.shuffle()
        val ordering = mutableListOf<QueueOrder>()

        for (position in 0 until songList.size) {
            val nullSafeSong = songList[position]
            if (nullSafeSong != null) {
                ordering.add(QueueOrder(orders[position], nullSafeSong))
            }
        }

        Thread.currentThread().realmInstance.executeTransaction { realmTransactionInstance ->
            realmTransactionInstance.copyToRealm(ordering)
        }

    }//todo other than random

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