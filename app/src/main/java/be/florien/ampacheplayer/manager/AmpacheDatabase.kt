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
        val realm = Realm.getDefaultInstance()
        return Observable
                .fromCallable {
                    val realmResults = realm.where(Song::class.java).findAllSorted("id")
                    realmResults
                }
                .flatMap {
                    realmResult ->
                    Observable.fromIterable(realmResult)
                }
                .buffer(50)
    }

    fun getArtists(): Observable<List<Artist>> {
        val realm = Realm.getDefaultInstance()
        return Observable
                .fromCallable {
                    val realmResults = realm.where(Artist::class.java).findAllSorted("id")
                    realmResults
                }
                .flatMap {
                    realmResult ->
                    Observable.fromIterable(realmResult)
                }
                .buffer(50)
                .doOnComplete {
                    realm.close()
                }
    }

    fun getAlbums(): Observable<List<Album>> {
        val realm = Realm.getDefaultInstance()
        return Observable
                .fromCallable {
                    val realmResults = realm.where(Album::class.java).findAllSorted("id")
                    realmResults
                }
                .flatMap {
                    realmResult ->
                    Observable.fromIterable(realmResult)
                }
                .buffer(50)
                .doOnComplete {
                    realm.close()
                }
    }

    fun getTags(): Observable<List<Tag>> {
        val realm = Realm.getDefaultInstance()
        return Observable
                .fromCallable {
                    val realmResults = realm.where(Tag::class.java).findAllSorted("id")
                    realmResults
                }
                .flatMap {
                    realmResult ->
                    Observable.fromIterable(realmResult)
                }
                .buffer(50)
                .doOnComplete {
                    realm.close()
                }
    }

    fun getPlaylists(): Observable<List<Playlist>> {
        val realm = Realm.getDefaultInstance()
        return Observable
                .fromCallable {
                    val realmResults = realm.where(Playlist::class.java).findAllSorted("id")
                    realmResults
                }
                .flatMap {
                    realmResult ->
                    Observable.fromIterable(realmResult)
                }
                .buffer(50)
                .doOnComplete {
                    realm.close()

                }
    }

    fun getSong(uid: Long): Observable<Song> {
        val realm = Realm.getDefaultInstance()
        return Observable
                .fromCallable {
                    val realmResults = realm.where(Song::class.java).equalTo("id", uid).findFirst()
                    realmResults
                }
                .doOnComplete {
                    realm.close()
                }
    }

    /**
     * Database setters
     */

    fun addSongs(songs: List<Song>): Observable<Unit> {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        realm ->
                        realm.copyToRealmOrUpdate(songs)
                    }
                    realm.close()
                }
    }

    fun addArtists(artists: List<Artist>): Observable<Unit> {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        realm ->
                        realm.copyToRealmOrUpdate(artists)
                    }
                    realm.close()
                }
    }

    fun addAlbums(albums: List<Album>): Observable<Unit> {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        realm ->
                        realm.copyToRealmOrUpdate(albums)
                    }
                    realm.close()
                }
    }

    fun addTags(tags: List<Tag>): Observable<Unit>? {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        realm ->
                        realm.copyToRealmOrUpdate(tags)
                    }
                    realm.close()
                }
    }

    fun addPlaylists(playlist: List<Playlist>): Observable<Unit>? {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        realm ->
                        realm.copyToRealmOrUpdate(playlist)
                    }
                    realm.close()
                }
    }
}