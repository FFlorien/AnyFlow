package be.florien.ampacheplayer.persistence

import android.content.SharedPreferences
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.extension.applyPutLong
import be.florien.ampacheplayer.extension.getDate
import be.florien.ampacheplayer.persistence.local.LocalDataManager
import be.florien.ampacheplayer.persistence.local.model.*
import be.florien.ampacheplayer.persistence.server.AmpacheConnection
import be.florien.ampacheplayer.persistence.server.model.AmpacheAlbumList
import be.florien.ampacheplayer.persistence.server.model.AmpacheArtistList
import be.florien.ampacheplayer.persistence.server.model.AmpacheError
import be.florien.ampacheplayer.persistence.server.model.AmpacheSongList
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

private const val LAST_SONG_UPDATE = "LAST_SONG_UPDATE"
private const val LAST_ARTIST_UPDATE = "LAST_ARTIST_UPDATE"
private const val LAST_ALBUM_UPDATE = "LAST_ALBUM_UPDATE"
private const val LAST_ALBUM_ARTIST_UPDATE = "LAST_ALBUM_ARTIST_UPDATE"

/**
 * Class updating the localDataManager in the process.
 */
@UserScope
class PersistenceManager
@Inject constructor(
        private val localDataManager: LocalDataManager,
        private val songServerConnection: AmpacheConnection,
        private val sharedPreferences: SharedPreferences) {

    private fun lastAcceptableUpdate() = Calendar.getInstance().apply {
        roll(Calendar.DAY_OF_YEAR, false)
    }

    /**
     * Getter with server updates
     */

    fun getSongs(): Observable<List<Song>> = getUpToDateList(
            LAST_SONG_UPDATE,
            AmpacheConnection::getSongs,
            LocalDataManager::getSongs,
            AmpacheSongList::error
    ) {
        localDataManager.addSongs(it.songs.map(::Song)).blockingAwait()
        localDataManager.getSongs().flatMapCompletable {
            saveQueueOrder(it)
        }
    }

    private fun saveQueueOrder(it: List<Song>): Completable {
        val queueOrder = mutableListOf<QueueOrder>()
        it.forEachIndexed { index, song ->
            queueOrder.add(QueueOrder(index, song))
        }
        return localDataManager.setOrder(queueOrder)
    }

    fun getGenres(): Observable<List<Song>> = getUpToDateList(
            LAST_SONG_UPDATE,
            AmpacheConnection::getSongs,
            LocalDataManager::getGenres,
            AmpacheSongList::error
    ) { localDataManager.addSongs(it.songs.map(::Song)) }

    fun getArtists(): Observable<List<Artist>> = getUpToDateList(
            LAST_ARTIST_UPDATE,
            AmpacheConnection::getArtists,
            LocalDataManager::getArtists,
            AmpacheArtistList::error
    ) { localDataManager.addArtists(it.artists.map(::Artist)) }

    fun getAlbums(): Observable<List<Album>> = getUpToDateList(
            LAST_ALBUM_UPDATE,
            AmpacheConnection::getAlbums,
            LocalDataManager::getAlbums,
            AmpacheAlbumList::error
    ) { localDataManager.addAlbums(it.albums.map(::Album)) }

    /**
     * Getters without server updates
     */

    fun getSongsInQueueOrder(): Flowable<List<Song>> = localDataManager.getSongsInQueueOrder()

    fun getFilters(): Flowable<List<Filter>> = localDataManager.getFilters()

//    fun getSongsForCurrentFilters(): Flowable<List<Song>> = localDataManager.getSongsForCurrentFilters()

    /**
     * Setters
     */

    fun clearFilters(): Completable = localDataManager.clearFilters()

    fun addFilters(filters: List<Filter>): Completable = localDataManager.addFilters(filters)

    /**
     * Private Method
     */

    private fun <ROOM_TYPE, SERVER_TYPE> getUpToDateList(
            updatePreferenceName: String,
            getListOnServer: AmpacheConnection.(Calendar) -> Observable<SERVER_TYPE>,
            getListOnDatabase: LocalDataManager.() -> Flowable<List<ROOM_TYPE>>,
            getError: SERVER_TYPE.() -> AmpacheError,
            saveToDatabase: (SERVER_TYPE) -> Completable)
            : Observable<List<ROOM_TYPE>> {
        val nowDate = Calendar.getInstance()
        val lastUpdate = sharedPreferences.getDate(updatePreferenceName, 0)
        val lastAcceptableUpdate = lastAcceptableUpdate()
        return if (lastUpdate.before(lastAcceptableUpdate)) {
            songServerConnection
                    .getListOnServer(lastUpdate)
                    .flatMap { result ->
                        saveToDatabase(result).blockingAwait()
                        sharedPreferences.applyPutLong(updatePreferenceName, nowDate.timeInMillis)
                        when (result.getError().code) {
                            401 -> songServerConnection.reconnect(songServerConnection.getListOnServer(lastUpdate))
                            else -> Observable.just(result)
                        }
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { localDataManager.getListOnDatabase().toObservable() }
        } else {
            localDataManager.getListOnDatabase().toObservable()
        }
    }
}