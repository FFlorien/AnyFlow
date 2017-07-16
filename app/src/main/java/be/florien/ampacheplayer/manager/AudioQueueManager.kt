package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.business.local.Song
import be.florien.ampacheplayer.business.realm.RealmSong
import javax.inject.Inject

/**
 * Manager for the queue of songs that are playing. It handle filters, random, repeat and addition to the queue
 */
class AudioQueueManager
@Inject constructor(val ampacheDatabase: AmpacheDatabase) {

    /**
     * Fields
     */
    var filters: List<Filter<*>> = mutableListOf()
    var listPosition: Int = 0
        set(value) {
            if (value >= currentAudioQueue.size) {
                throw IndexOutOfBoundsException("You've reach the last song in the queue")
            }
            field = value
        }
    private var currentAudioQueue = ampacheDatabase.getSongs(filters)

    /**
     * Methods
     */
    fun getAudioQueue(): List<Song> = currentAudioQueue.map(::Song)

    fun getCurrentSong(): RealmSong = currentAudioQueue[listPosition]

    fun addTitleFilter(title: String) {
        filters += Filter.TitleIs(title)
        currentAudioQueue = ampacheDatabase.getSongs(filters)
    }
}