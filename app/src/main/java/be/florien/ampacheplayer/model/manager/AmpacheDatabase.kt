package be.florien.ampacheplayer.model.manager

import be.florien.ampacheplayer.model.realm.*
import io.reactivex.Observable
import io.realm.Realm
import io.realm.RealmResults
import java.math.BigInteger

/**
 * Manager for the ampache API database-side //todo update description when needed
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
                .doOnComplete {
                    realm.close()
                }
    }

    fun getArtists(): Observable<List<Artist>> {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    val realmResults = realm.where(Artist::class.java).findAllSorted("id")
                    realm.close()
                    realmResults
                }
                .flatMap {
                    realmResult ->
                    Observable.fromIterable(realmResult)
                }
                .buffer(50)
    }

    fun getAlbums(): Observable<List<Album>> {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    val realmResults = realm.where(Album::class.java).findAllSorted("id")
                    realm.close()
                    realmResults
                }
                .flatMap {
                    realmResult ->
                    Observable.fromIterable(realmResult)
                }
                .buffer(50)
    }

    fun getTags(): Observable<List<Tag>> {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    val realmResults = realm.where(Tag::class.java).findAllSorted("id")
                    realm.close()
                    realmResults
                }
                .flatMap {
                    realmResult ->
                    Observable.fromIterable(realmResult)
                }
                .buffer(50)
    }

    fun getPlaylists(): Observable<List<Playlist>> {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    val realmResults = realm.where(Playlist::class.java).findAllSorted("id")
                    realm.close()
                    realmResults
                }
                .flatMap {
                    realmResult ->
                    Observable.fromIterable(realmResult)
                }
                .buffer(50)
    }

    fun getSong(uid: Long): Observable<Song> {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    val realmResults = realm.where(Song::class.java).equalTo("id", uid).findFirst()
                    realm.close()
                    realmResults
                }
    }

    /**
     * Database setters
     */
    fun addSongs(song: List<Song>): Observable<Unit> {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        realm ->
                        realm.insertOrUpdate(song)
                    }
                    realm.close()
                }
    }

    fun addArtist(artist: Artist): Observable<Unit>? {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        realm ->
                        realm.insertOrUpdate(artist)
                    }
                    realm.close()
                }
    }

    fun addAlbum(album: Album): Observable<Unit>? {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        realm ->
                        realm.insertOrUpdate(album)
                    }
                    realm.close()
                }
    }

    fun addTag(tag: Tag): Observable<Unit>? {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        realm ->
                        realm.insertOrUpdate(tag)
                    }
                    realm.close()
                }
    }

    fun addPlaylist(playlist: Playlist): Observable<Unit>? {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        realm ->
                        realm.insertOrUpdate(playlist)
                    }
                    realm.close()
                }
    }

    /**
     * Private Methods
     */
    private fun binToHex(data: ByteArray): String {
        return String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
    }
}