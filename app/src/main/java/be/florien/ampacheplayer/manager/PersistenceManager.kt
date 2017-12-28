package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.business.realm.Song
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Class updating the databaseManager in the process.
 */
class PersistenceManager
@Inject constructor(
        private var databaseManager: DatabaseManager,
        private var connection: AmpacheConnection) {

    /**
     * Getter
     */

    fun refreshSongs(): Observable<Boolean> = connection.getSongs()
            .flatMap { result ->
                when (result.error.code) {
                    401 -> connection.reconnect(connection.getSongs())
                    else -> Observable.just(result)
                }
            }
            .flatMap { songs ->
                databaseManager.addSongs(songs.songs.map(::Song))
                Observable.just(true)
            }
}