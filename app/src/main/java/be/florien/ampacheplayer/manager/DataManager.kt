package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.business.local.Song
import be.florien.ampacheplayer.business.realm.RealmSong
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Class managing all request for data, handling caching and updating the database in the process.
 */
class DataManager
@Inject constructor(
        var database: AmpacheDatabase,
        var connection: AmpacheConnection) {

    /**
     * Getter
     */

    fun refreshSongs(): Observable<Boolean> {
        return connection
                .getSongs()
                .flatMap {
                    result ->
                    when (result.error.code) {
                        401 -> connection.reconnect(connection.getSongs())
                        else -> Observable.just(result)
                    }
                }
                .flatMap {
                    songs ->
                    database.addSongs(songs.songs.map(::RealmSong))
                    Observable.just(true)
                }
    }

    fun getSong(id: Long): Observable<Song> = connection.getSong(id).flatMap { Observable.just(Song(it.songs[0])) }
}