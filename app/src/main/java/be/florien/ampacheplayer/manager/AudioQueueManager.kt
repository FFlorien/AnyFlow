package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.business.local.Song
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Manager for the queue of songs that are playing. It handle filters, random, repeat and addition to the queue
 */
class AudioQueueManager
@Inject constructor(val dataManager: DataManager) {

    var filters: List<Filter<*>> = mutableListOf()

    fun getAudioQueue(): Observable<List<Song>> {
        return dataManager.getSongs(filters)
    }

    fun addTitleFilter(title: String) {
        filters += Filter.TitleIs(title)
    }
}