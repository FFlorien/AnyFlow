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
    fun getSongs(): Observable<List<Song>> {
        return Observable
                .fromCallable { Realm.getDefaultInstance().where(Song::class.java).findAllSorted("id") }
                .flatMap { realmResult -> Observable.fromIterable(realmResult) }
                .buffer(50)
    }

    fun getArtists(): Observable<List<Artist>> = Realm.getDefaultInstance().let {
        Observable
                .fromCallable { it.where(Artist::class.java).findAllSorted("id") }
                .flatMap { realmResult -> Observable.fromIterable(realmResult) }
                .buffer(50)
                .doOnComplete { it.close() }
    }

    fun getAlbums(): Observable<List<Album>> = Realm.getDefaultInstance().let {
        Observable
                .fromCallable { it.where(Album::class.java).findAllSorted("id") }
                .flatMap { realmResult -> Observable.fromIterable(realmResult) }
                .buffer(50)
                .doOnComplete { it.close() }
    }

    fun getTags(): Observable<List<Tag>> = Realm.getDefaultInstance().let {
        Observable
                .fromCallable { it.where(Tag::class.java).findAllSorted("id") }
                .flatMap { realmResult -> Observable.fromIterable(realmResult) }
                .buffer(50)
                .doOnComplete { it.close() }
    }

    fun getPlaylists(): Observable<List<Playlist>> = Realm.getDefaultInstance().let {
        Observable
                .fromCallable { it.where(Playlist::class.java).findAllSorted("id") }
                .flatMap { realmResult -> Observable.fromIterable(realmResult) }
                .buffer(50)
                .doOnComplete { it.close() }
    }

    fun getSong(uid: Long): Observable<Song> = Realm.getDefaultInstance().let {
        Observable
                .fromCallable { it.where(Song::class.java).equalTo("id", uid).findFirst() }
                .doOnComplete { it.close() }
    }

    /**
     * Database setters
     */

    fun addSongs(songs: List<Song>): Observable<Unit> = Observable
            .fromCallable {
                Realm.getDefaultInstance().let {
                    it.executeTransaction { realm -> realm.copyToRealmOrUpdate(songs) }
                    it.close()
                }
            }

    fun addArtists(artists: List<Artist>): Observable<Unit> = Observable
            .fromCallable {
                Realm.getDefaultInstance().let {
                    it.executeTransaction { realm -> realm.copyToRealmOrUpdate(artists) }
                    it.close()
                }
            }

    fun addAlbums(albums: List<Album>): Observable<Unit> = Observable
            .fromCallable {
                Realm.getDefaultInstance().let {
                    it.executeTransaction { realm -> realm.copyToRealmOrUpdate(albums) }
                    it.close()
                }
            }

    fun addTags(tags: List<Tag>): Observable<Unit>? = Observable
            .fromCallable {
                Realm.getDefaultInstance().let {
                    it.executeTransaction { realm -> realm.copyToRealmOrUpdate(tags) }
                    it.close()
                }
            }

    fun addPlaylists(playlist: List<Playlist>): Observable<Unit>? = Observable
            .fromCallable {
                Realm.getDefaultInstance().let {
                    it.executeTransaction { realm -> realm.copyToRealmOrUpdate(playlist) }
                    it.close()
                }
            }
}