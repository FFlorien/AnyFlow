package be.florien.ampacheplayer.persistence

import android.content.SharedPreferences
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.extension.applyPutLong
import be.florien.ampacheplayer.extension.getDate
import be.florien.ampacheplayer.persistence.local.LocalDataManager
import be.florien.ampacheplayer.persistence.local.model.Album
import be.florien.ampacheplayer.persistence.local.model.Artist
import be.florien.ampacheplayer.persistence.local.model.Song
import be.florien.ampacheplayer.persistence.server.AmpacheConnection
import be.florien.ampacheplayer.persistence.server.model.AmpacheAlbumList
import be.florien.ampacheplayer.persistence.server.model.AmpacheArtistList
import be.florien.ampacheplayer.persistence.server.model.AmpacheError
import be.florien.ampacheplayer.persistence.server.model.AmpacheSongList
import io.reactivex.Completable
import io.reactivex.Observable
import java.util.*
import javax.inject.Inject

private const val LAST_SONG_UPDATE = "LAST_SONG_UPDATE"
private const val LAST_ARTIST_UPDATE = "LAST_ARTIST_UPDATE"
private const val LAST_ALBUM_UPDATE = "LAST_ALBUM_UPDATE"
private const val LAST_ALBUM_ARTIST_UPDATE = "LAST_ALBUM_ARTIST_UPDATE"

/**
 * Update the local data with the one from the server
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

    fun updateSongs(): Completable= getUpToDateList(
            LAST_SONG_UPDATE,
            AmpacheConnection::getSongs,
            AmpacheSongList::error
    ) { localDataManager.addSongs(it.songs.map(::Song))}

    fun updateArtists(): Completable= getUpToDateList(
            LAST_ARTIST_UPDATE,
            AmpacheConnection::getArtists,
            AmpacheArtistList::error
    ) { localDataManager.addArtists(it.artists.map(::Artist)) }

    fun updateAlbums(): Completable= getUpToDateList(
            LAST_ALBUM_UPDATE,
            AmpacheConnection::getAlbums,
            AmpacheAlbumList::error
    ) { localDataManager.addAlbums(it.albums.map(::Album)) }

    /**
     * Private Method
     */

    private fun <SERVER_TYPE> getUpToDateList(
            updatePreferenceName: String,
            getListOnServer: AmpacheConnection.(Calendar) -> Observable<SERVER_TYPE>,
            getError: SERVER_TYPE.() -> AmpacheError,
            saveToDatabase: (SERVER_TYPE) -> Completable)
            : Completable {
        val nowDate = Calendar.getInstance()
        val lastUpdate = sharedPreferences.getDate(updatePreferenceName, 0)
        val lastAcceptableUpdate = lastAcceptableUpdate()
        return if (lastUpdate.before(lastAcceptableUpdate)) {
            songServerConnection
                    .getListOnServer(lastUpdate)
                    .flatMapCompletable { result ->
                        saveToDatabase(result).doFinally {
                            when (result.getError().code) {
                                401 -> songServerConnection.reconnect(songServerConnection.getListOnServer(lastUpdate))
                                else -> Observable.just(result)
                            }
                        }
                    }.doOnComplete {
                        sharedPreferences.applyPutLong(updatePreferenceName, nowDate.timeInMillis)
                    }
        } else {
            Completable.complete()
        }
    }
}