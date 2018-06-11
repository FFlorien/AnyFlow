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
import be.florien.ampacheplayer.player.Filter
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
 * Class updating the songsDatabase in the process.
 */
@UserScope
class PersistenceManager
@Inject constructor(
        private val songsDatabase: SongsDatabase,
        private val songServerConnection: AmpacheConnection,
        private val sharedPreferences: SharedPreferences) {

    private fun lastAcceptableUpdate() = Calendar.getInstance().apply {
        roll(Calendar.DAY_OF_YEAR, false)
    }

    /**
     * Getter
     */

    fun getSongs(): Observable<List<Song>> = getUpToDateList(
            LAST_SONG_UPDATE,
            AmpacheConnection::getSongs,
            SongsDatabase::getSongs,
            AmpacheSongList::error,
            { songsDatabase.addSongs(it.songs.map(::Song)) })

    fun getGenres(): Observable<List<Song>> = getUpToDateList(
            LAST_SONG_UPDATE,
            AmpacheConnection::getSongs,
            SongsDatabase::getGenres,
            AmpacheSongList::error,
            { songsDatabase.addSongs(it.songs.map(::Song)) })

    fun getArtists(): Observable<List<Artist>> = getUpToDateList(
            LAST_ARTIST_UPDATE,
            AmpacheConnection::getArtists,
            SongsDatabase::getArtists,
            AmpacheArtistList::error,
            { songsDatabase.addArtists(it.artists.map(::Artist)) })

    fun getAlbums(): Observable<List<Album>> = getUpToDateList(
            LAST_ALBUM_UPDATE,
            AmpacheConnection::getAlbums,
            SongsDatabase::getAlbums,
            AmpacheAlbumList::error,
            { songsDatabase.addAlbums(it.albums.map(::Album)) })

    /**
     * Setter
     */

    fun <T> addFilter(filter: Filter<T>) {

    }

    /**
     * Private Method
     */

    private fun <ROOM_TYPE, SERVER_TYPE> getUpToDateList(
            updatePreferenceName: String,
            getListOnServer: AmpacheConnection.(Calendar) -> Observable<SERVER_TYPE>,
            getListOnDatabase: SongsDatabase.() -> Observable<List<ROOM_TYPE>>,
            getError: SERVER_TYPE.() -> AmpacheError,
            saveToDatabase: (SERVER_TYPE) -> Unit)
            : Observable<List<ROOM_TYPE>> {
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
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { songsDatabase.getListOnDatabase() }
        } else {
            songsDatabase.getListOnDatabase()
        }
    }
}