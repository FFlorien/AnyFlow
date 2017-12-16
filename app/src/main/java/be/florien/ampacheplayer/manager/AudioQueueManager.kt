package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.business.realm.Song
import io.reactivex.subjects.PublishSubject
import io.realm.Realm
import io.realm.RealmResults
import javax.inject.Inject

const val NO_CURRENT_SONG = -13456

/**
 * Manager for the queue of songs that are playing. It handle filters, random, repeat and addition to the queue
 */
class AudioQueueManager
@Inject constructor(private val databaseManager: DatabaseManager) {

    /**
     * Fields
     */
    private var filters: List<Filter<*>> = mutableListOf()
    val changeListener: PublishSubject<Int> = PublishSubject.create()
    val itemsCount: Int = databaseManager.getSongs(filters).size
    var listPosition: Int = NO_CURRENT_SONG
        set(value) {
            field = if (value in 0 until databaseManager.getSongs(filters).size) {
                value
            } else {
                NO_CURRENT_SONG
            }
            changeListener.onNext(field)
        }


    /**
     * Methods
     */
    fun getCurrentSong(realmInstance: Realm): Song = databaseManager.getSongs(filters, realmInstance)[listPosition]

    fun getCurrentAudioQueue(): RealmResults<Song> = databaseManager.getSongs(filters)
}