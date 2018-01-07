package be.florien.ampacheplayer.persistence

import be.florien.ampacheplayer.persistence.model.Song
import be.florien.ampacheplayer.api.AmpacheConnection
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Class updating the databaseManager in the process.
 */
class PersistenceManager
@Inject constructor(
        private var databaseManager: DatabaseManager,
        private var songServerConnection: AmpacheConnection) {

    /**
     * Getter
     */

    fun refreshSongs(): Observable<Boolean> = songServerConnection.getSongs()
            .flatMap { result ->
                when (result.error.code) {
                    401 -> songServerConnection.reconnect(songServerConnection.getSongs())
                    else -> Observable.just(result)
                }
            }
            .flatMap { songs ->
                databaseManager.addSongs(songs.songs.map(::Song))
                Observable.just(true)
            }
}