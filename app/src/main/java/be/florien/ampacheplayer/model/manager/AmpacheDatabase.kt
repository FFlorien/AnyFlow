package be.florien.ampacheplayer.model.manager

import be.florien.ampacheplayer.App
import be.florien.ampacheplayer.model.data.*
import io.reactivex.Observable
import io.realm.Realm
import java.math.BigInteger

/**
 * Manager for the ampache API database-side //todo update description when needed
 */
class AmpacheDatabase {
    /**
     * Constructors
     */
    init {
        App.ampacheComponent.inject(this)
    }

    /**
     * Database getters
     */
    fun getSongs(): Observable<Song> {
        return Observable
                .fromCallable {
                    val realm = Realm.getDefaultInstance()
                    val realmResults = realm.where(Song::class.java).findAllSorted("id")
                    realm.close()
                    realmResults
                }
                .flatMap {
                    realmResult ->
                    Observable.fromIterable(realmResult)
                }
    }

    fun getArtists(): Observable<Artist> {
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
    }

    fun getAlbums(): Observable<Album> {
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
    }

    fun getTags(): Observable<Tag> {
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
    }

    fun getPlaylists(): Observable<Playlist> {
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

    private fun binToHex(data: ByteArray): String {
        return String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
    }
}