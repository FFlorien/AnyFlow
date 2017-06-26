package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.model.realm.*
import io.realm.Realm

/**
 * Manager for the ampache data database-side
 */
class AmpacheDatabase {
    /**
     * Database getters
     */
    fun getSongs(): List<RealmSong> = Realm.getDefaultInstance().let {
        val realmResult = it.where(RealmSong::class.java).findAllSorted("id")
        it.close()
        ArrayList(realmResult)
    }

    fun getArtists(): List<RealmArtist> = Realm.getDefaultInstance().let {
        val realmResult = it.where(RealmArtist::class.java).findAllSorted("id")
        it.close()
        ArrayList(realmResult)
    }

    fun getAlbums(): List<RealmAlbum> = Realm.getDefaultInstance().let {
        val realmResult = it.where(RealmAlbum::class.java).findAllSorted("id")
        it.close()
        ArrayList(realmResult)
    }

    fun getTags(): List<RealmTag> = Realm.getDefaultInstance().let {
        val realmResult = it.where(RealmTag::class.java).findAllSorted("id")
        it.close()
        ArrayList(realmResult)
    }

    fun getPlayLists(): List<RealmPlaylist> = Realm.getDefaultInstance().let {
        val realmResult = it.where(RealmPlaylist::class.java).findAllSorted("id")
        it.close()
        ArrayList(realmResult)
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
}