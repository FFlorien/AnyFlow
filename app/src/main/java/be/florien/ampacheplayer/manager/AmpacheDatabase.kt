package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.business.realm.*
import io.realm.Realm
import io.realm.RealmQuery

/**
 * Manager for the ampache data database-side
 */
class AmpacheDatabase {
    /**
     * Database getters
     */
    fun getSongs(filters: List<Filter<*>> = emptyList()): List<RealmSong> = Realm.getDefaultInstance().let {
        val realmQuery = it.where(RealmSong::class.java)
        getRealmObjects(filters, realmQuery, it)
    }

    /**
     * Database setters
     */

    fun addSongs(songs: List<RealmSong>): Unit =
            Realm.getDefaultInstance().let {
                it.beginTransaction()
                it.copyToRealmOrUpdate(songs)
                it.commitTransaction()
            }

    fun addArtists(artists: List<RealmArtist>): Unit =
            Realm.getDefaultInstance().let {
                it.executeTransaction { realm -> realm.copyToRealmOrUpdate(artists) }
                it.close()
            }

    fun addAlbums(albums: List<RealmAlbum>): Unit =
            Realm.getDefaultInstance().let {
                it.executeTransaction { realm -> realm.copyToRealmOrUpdate(albums) }
                it.close()
            }

    fun addTags(tags: List<RealmTag>): Unit =
            Realm.getDefaultInstance().let {
                it.executeTransaction { realm -> realm.copyToRealmOrUpdate(tags) }
                it.close()
            }

    fun addPlayLists(playlist: List<RealmPlaylist>): Unit =
            Realm.getDefaultInstance().let {
                it.executeTransaction { realm -> realm.copyToRealmOrUpdate(playlist) }
                it.close()
            }

    /**
     * Private methods
     */

    private fun getRealmObjects(filters: List<Filter<*>>, realmQuery: RealmQuery<RealmSong>, realmInstance: Realm): ArrayList<RealmSong> {
        var isFirstFilter = true
        for (filter in filters) {
            applyFilter(realmQuery, filter, isFirstFilter)
            isFirstFilter = false
        }
        val realmResult = realmQuery.findAllSorted("id")
        realmInstance.close()
        return ArrayList(realmResult)
    }

    private fun applyFilter(realmQuery: RealmQuery<RealmSong>, filter: Filter<*>, isFirst: Boolean) {
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