package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.business.realm.Song
import io.reactivex.subjects.PublishSubject
import io.realm.Realm
import io.realm.RealmResults
import javax.inject.Inject

/**
 * Manager for the queue of songs that are playing. It handle filters, random, repeat and addition to the queue
 */
class AudioQueueManager
@Inject constructor(private val databaseManager: DatabaseManager) {

    /**
     * Fields
     */
    val changeListener: PublishSubject<Int> = PublishSubject.create()
    var listPosition: Int = 0
        set(value) {
            if (value >= databaseManager.getSongs(filters).size) {
                throw IndexOutOfBoundsException("You've reach the last song in the queue")
            }
            field = value
            changeListener.onNext(value)
        }
    private var filters: List<Filter<*>> = mutableListOf()


    /**
     * Methods
     */
    fun getCurrentSong(realmInstance: Realm): Song = databaseManager.getSongs(filters, realmInstance)[listPosition]

    fun getCurrentAudioQueue(): RealmResults<Song> = databaseManager.getSongs(filters)
}