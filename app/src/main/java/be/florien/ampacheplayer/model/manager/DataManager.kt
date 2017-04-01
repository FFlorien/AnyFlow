package be.florien.ampacheplayer.model.manager

import android.content.SharedPreferences
import be.florien.ampacheplayer.App
import be.florien.ampacheplayer.model.realm.Song
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by florien on 31/03/17.
 */
class DataManager {
    /**
     * Constants
     */
    private val LAST_SONG_UPDATE_NAME = "lastSongUpdate"

    /**
     * Fields
     */
    @Inject
    lateinit var database: AmpacheDatabase
    @Inject
    lateinit var connection: AmpacheConnection
    @Inject
    lateinit var prefs: SharedPreferences

    private var lastUpdate = "1970-01-01"


    /**
     * Constructor
     */
    init {
        App.ampacheComponent.inject(this)
        lastUpdate = prefs.getString(LAST_SONG_UPDATE_NAME, lastUpdate)
    }

    /**
     * Getter
     */

    fun getSongs(): Observable<List<Song>> {
        return connection
                .getSongs(lastUpdate)
                .flatMap {
                    songs ->
                    database.addSongs(songs.songs.map(::Song))
                }
                .flatMap {
                    database.getSongs()
                }
    }
}