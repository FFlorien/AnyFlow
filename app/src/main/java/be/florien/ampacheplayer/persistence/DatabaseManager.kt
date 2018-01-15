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
@Inject constructor(val realmRead: Realm) {
    /**
     * Database getters
     */
    fun getSongs(filters: List<Filter<*>> = emptyList(), realmInstance: Realm = realmRead): RealmResults<Song> = realmInstance.let {
        val realmQuery = it.where(Song::class.java)
        var isFirstFilter = true
        for (filter in filters) {
            applyFilter(realmQuery, filter, isFirstFilter)
            isFirstFilter = false
        }
        return realmQuery.findAllSorted("id")
    }

    fun getGenres(realmInstance: Realm = realmRead): List<String> = realmInstance.let {
        val distinctGenres = mutableListOf<String>()
        val realmQuery = it.where(Song::class.java)
        realmQuery.distinct("genre").mapTo(distinctGenres) { it.genre }
        return distinctGenres
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