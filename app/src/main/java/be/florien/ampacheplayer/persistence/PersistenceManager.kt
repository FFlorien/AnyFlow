package be.florien.ampacheplayer.persistence

import android.content.SharedPreferences
import be.florien.ampacheplayer.api.AmpacheConnection
import be.florien.ampacheplayer.api.model.AmpacheAlbumList
import be.florien.ampacheplayer.api.model.AmpacheArtistList
import be.florien.ampacheplayer.api.model.AmpacheError
import be.florien.ampacheplayer.api.model.AmpacheSongList
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.extension.applyPutLong
import be.florien.ampacheplayer.extension.getDate
import be.florien.ampacheplayer.persistence.model.Album
import be.florien.ampacheplayer.persistence.model.Artist
import be.florien.ampacheplayer.persistence.model.Song
import io.reactivex.Flowable
import io.reactivex.Observable
import io.realm.RealmObject
import io.realm.RealmResults
import java.util.*
import javax.inject.Inject

private const val LAST_SONG_UPDATE = "LAST_SONG_UPDATE"
private const val LAST_ARTIST_UPDATE = "LAST_ARTIST_UPDATE"
private const val LAST_ALBUM_UPDATE = "LAST_ALBUM_UPDATE"
private const val LAST_ALBUM_ARTIST_UPDATE = "LAST_ALBUM_ARTIST_UPDATE"

/**
 * Class updating the databaseManager in the process.
 */
@UserScope
class PersistenceManager
@Inject constructor(
        private val databaseManager: DatabaseManager,
        private val songServerConnection: AmpacheConnection,
        private val sharedPreferences: SharedPreferences) {

    private fun lastAcceptableUpdate() = Calendar.getInstance().apply {
        roll(Calendar.DAY_OF_YEAR, false)
    }

    /**
     * Getter
     */

    fun getSongs(): Observable<RealmResults<Song>> = getUpToDateList(
                LAST_SONG_UPDATE,
                AmpacheConnection::getSongs,
                DatabaseManager::getSongs,
                AmpacheSongList::error,
                {databaseManager.addSongs(it.songs.map (::Song))})

    fun getGenres(): Observable<RealmResults<Song>> = getUpToDateList(
                LAST_SONG_UPDATE,
                AmpacheConnection::getSongs,
                DatabaseManager::getGenres,
                AmpacheSongList::error,
                {databaseManager.addSongs(it.songs.map (::Song))})

    fun getArtists(): Observable<RealmResults<Artist>> = getUpToDateList(
                LAST_ARTIST_UPDATE,
                AmpacheConnection::getArtists,
                DatabaseManager::getArtists,
                AmpacheArtistList::error,
                {databaseManager.addArtists(it.artists.map (::Artist))})

    fun getAlbums(): Observable<RealmResults<Album>> = getUpToDateList(
                LAST_ALBUM_UPDATE,
                AmpacheConnection::getAlbums,
                DatabaseManager::getAlbums,
                AmpacheAlbumList::error,
                {databaseManager.addAlbums(it.albums.map (::Album))})

    /**
     * Private Method
     */

    private fun <REALM_TYPE: RealmObject, SERVER_TYPE> getUpToDateList(
            updatePreferenceName: String,
            getListOnServer: AmpacheConnection.(Calendar) -> Observable<SERVER_TYPE>,
            getListOnDatabase: DatabaseManager.() -> RealmResults<REALM_TYPE>,
            getError: SERVER_TYPE.() -> AmpacheError,
            saveToDatabase: (SERVER_TYPE) -> Unit)
            : Observable<RealmResults<REALM_TYPE>> {
        val nowDate = Calendar.getInstance()
        val lastUpdate = sharedPreferences.getDate(updatePreferenceName, 0)
        val lastAcceptableUpdate = lastAcceptableUpdate()
        return if (lastUpdate.before(lastAcceptableUpdate)) {
            songServerConnection
                    .getListOnServer(lastUpdate)
                    .flatMap { result ->
                        saveToDatabase(result)
                        sharedPreferences.applyPutLong(updatePreferenceName, nowDate.timeInMillis)
                        when (result.getError().code) {
                            401 -> songServerConnection.reconnect(songServerConnection.getListOnServer(lastUpdate))
                            else -> Observable.just(result)
                        }
                    }
                    .doOnNext(saveToDatabase)
                    .flatMap { databaseManager.getListOnDatabase().asFlowable().toObservable() }
        } else {
            databaseManager.getListOnDatabase().asFlowable().toObservable()
        }
    }
}