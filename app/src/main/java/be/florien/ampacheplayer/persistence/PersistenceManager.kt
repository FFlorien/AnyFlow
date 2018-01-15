package be.florien.ampacheplayer.persistence

import android.content.SharedPreferences
import be.florien.ampacheplayer.api.AmpacheConnection
import be.florien.ampacheplayer.extension.applyPutLong
import be.florien.ampacheplayer.extension.getDate
import be.florien.ampacheplayer.persistence.model.Artist
import be.florien.ampacheplayer.persistence.model.Song
import io.reactivex.Flowable
import io.reactivex.Observable
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

    fun getSongs(): Flowable<RealmResults<Song>> {
        val nowDate = Calendar.getInstance()
        val lastUpdate = sharedPreferences.getDate(LAST_SONG_UPDATE, 0)
        val lastAcceptableUpdate = lastAcceptableUpdate()
        return if (lastUpdate.before(lastAcceptableUpdate)) {
            songServerConnection
                    .getSongs(from = nowDate)
                    .doOnNext {
                        databaseManager.addSongs(it.songs.map(::Song))
                    }
                    .flatMap { result ->
                        when (result.error.code) {
                            401 -> songServerConnection.reconnect(songServerConnection.getSongs())
                            else -> Observable.just(result)
                        }
                    }
                    .doOnComplete { sharedPreferences.applyPutLong(LAST_SONG_UPDATE, nowDate.timeInMillis) }
                    .to { databaseManager.getSongs().asFlowable() }

        } else {
            databaseManager.getSongs().asFlowable()
        }

    }

    fun getArtists(): Observable<Artist> = songServerConnection.getArtists()
            .flatMap { result ->
                when (result.error.code) {
                    401 -> songServerConnection.reconnect(songServerConnection.getArtists())
                    else -> Observable.just(result)
                }
            }
            .flatMap { artists ->
                databaseManager.addArtists(artists.artists.map(::Artist))
                databaseManager.getArtists().asObservable()
            }
}