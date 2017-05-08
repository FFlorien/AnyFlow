package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.model.realm.*
import io.reactivex.Observable
import io.realm.Realm

/**
 * Manager for the ampache data database-side
 */
class AmpacheDatabase {
    /**
     * Database getters
     */
    fun getSongs(): Observable<List<RealmSong>> {
        return Observable
                .fromCallable { Realm.getDefaultInstance().where(RealmSong::class.java).findAllSorted("id") }
                .flatMap { realmResult -> Observable.just(ArrayList(realmResult)) }
    }

    fun getArtists(): Observable<List<RealmArtist>> = Realm.getDefaultInstance().let {
        Observable
                .fromCallable { it.where(RealmArtist::class.java).findAllSorted("id") }
                .flatMap { realmResult -> Observable.just(ArrayList(realmResult) as List<RealmArtist>) }
                .doOnComplete { it.close() }
    }

    fun getAlbums(): Observable<List<RealmAlbum>> = Realm.getDefaultInstance().let {
        Observable
                .fromCallable { it.where(RealmAlbum::class.java).findAllSorted("id") }
                .flatMap { realmResult -> Observable.just(ArrayList(realmResult) as List<RealmAlbum>) }
                .doOnComplete { it.close() }
    }

    fun getTags(): Observable<List<RealmTag>> = Realm.getDefaultInstance().let {
        Observable
                .fromCallable { it.where(RealmTag::class.java).findAllSorted("id") }
                .flatMap { realmResult -> Observable.just(ArrayList(realmResult) as List<RealmTag>) }
                .doOnComplete { it.close() }
    }

    fun getPlayLists(): Observable<List<RealmPlaylist>> = Realm.getDefaultInstance().let {
        Observable
                .fromCallable { it.where(RealmPlaylist::class.java).findAllSorted("id") }
                .flatMap { realmResult -> Observable.just(ArrayList(realmResult) as List<RealmPlaylist>) }
                .doOnComplete { it.close() }
    }

    fun getSong(uid: Long): Observable<RealmSong> = Realm.getDefaultInstance().let {
        Observable
                .fromCallable { it.where(RealmSong::class.java).equalTo("id", uid).findFirst() }
                .doOnComplete { it.close() }
    }

    /**
     * Database setters
     */

    fun addSongs(songs: List<RealmSong>): Observable<Unit> = Observable
            .fromCallable {
                Realm.getDefaultInstance().let {
                    it.beginTransaction()
                    it.copyToRealmOrUpdate(songs)
                    it.commitTransaction()
                    it.close()
                }
            }

    fun addArtists(artists: List<RealmArtist>): Observable<Unit> = Observable
            .fromCallable {
                Realm.getDefaultInstance().let {
                    it.executeTransaction { realm -> realm.copyToRealmOrUpdate(artists) }
                    it.close()
                }
            }

    fun addAlbums(albums: List<RealmAlbum>): Observable<Unit> = Observable
            .fromCallable {
                Realm.getDefaultInstance().let {
                    it.executeTransaction { realm -> realm.copyToRealmOrUpdate(albums) }
                    it.close()
                }
            }

    fun addTags(tags: List<RealmTag>): Observable<Unit>? = Observable
            .fromCallable {
                Realm.getDefaultInstance().let {
                    it.executeTransaction { realm -> realm.copyToRealmOrUpdate(tags) }
                    it.close()
                }
            }

    fun addPlayLists(playlist: List<RealmPlaylist>): Observable<Unit>? = Observable
            .fromCallable {
                Realm.getDefaultInstance().let {
                    it.executeTransaction { realm -> realm.copyToRealmOrUpdate(playlist) }
                    it.close()
                }
            }
}