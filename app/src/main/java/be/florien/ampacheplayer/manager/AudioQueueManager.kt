package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.model.local.Song
import be.florien.ampacheplayer.model.queue.Filter
import be.florien.ampacheplayer.model.realm.RealmSong
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by florien on 2/07/17.
 */
class AudioQueueManager
@Inject constructor(val dataManager: DataManager) {

    var filters: List<Filter<RealmSong, Any>> = mutableListOf()

    fun getAudioQueue(): Observable<List<Song>> {
        return dataManager.getSongs(filters)
    }
}