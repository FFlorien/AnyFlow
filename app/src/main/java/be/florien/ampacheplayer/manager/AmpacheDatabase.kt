package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.model.queue.Filter
import be.florien.ampacheplayer.model.realm.*
import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmQuery

/**
 * Manager for the ampache data database-side
 */
class AmpacheDatabase {
    /**
     * Database getters
     */
    fun getSongs(filters: List<Filter<RealmSong, Any>> = emptyList()): List<RealmSong> = Realm.getDefaultInstance().let {
        val realmQuery = it.where(RealmSong::class.java)
        getRealmObjects(filters, realmQuery, it)
    }

    fun getArtists(filters: List<Filter<RealmArtist, Any>> = emptyList()): List<RealmArtist> = Realm.getDefaultInstance().let {
        val realmQuery = it.where(RealmArtist::class.java)
        getRealmObjects(filters, realmQuery, it)
    }

    fun getAlbums(filters: List<Filter<RealmAlbum, Any>> = emptyList()): List<RealmAlbum> = Realm.getDefaultInstance().let {
        val realmQuery = it.where(RealmAlbum::class.java)
        getRealmObjects(filters, realmQuery, it)
    }

    fun getPlayLists(filters: List<Filter<RealmPlaylist, Any>> = emptyList()): List<RealmPlaylist> = Realm.getDefaultInstance().let {
        val realmQuery = it.where(RealmPlaylist::class.java)
        getRealmObjects(filters, realmQuery, it)
    }

    fun getTags(filters: List<Filter<RealmTag, Any>> = emptyList()): List<RealmTag> = Realm.getDefaultInstance().let {
        val realmQuery = it.where(RealmTag::class.java)
        getRealmObjects(filters, realmQuery, it)
    }

    fun getSong(uid: Long): RealmSong = Realm.getDefaultInstance().let {
        val findFirst = it.where(RealmSong::class.java).equalTo("id", uid).findFirst()
        it.close()
        findFirst
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


    private fun <T : RealmObject> getRealmObjects(filters: List<Filter<T, Any>>, realmQuery: RealmQuery<T>, it: Realm): ArrayList<T> {
        for (filter in filters) {
            filter.apply {
                filterFunction.invoke(realmQuery, fieldName, argument)
            }
        }
        val realmResult = realmQuery.findAllSorted("id")
        it.close()
        return ArrayList(realmResult)
    }
}