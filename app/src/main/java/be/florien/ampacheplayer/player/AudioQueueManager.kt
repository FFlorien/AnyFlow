package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.persistence.model.Song
import be.florien.ampacheplayer.persistence.DatabaseManager
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
    val positionObservable: PublishSubject<Int> = PublishSubject.create()
    val itemsCount: Int = databaseManager.getSongs(filters).size
    var listPosition: Int = NO_CURRENT_SONG
        set(value) {
            field = if (value in 0 until databaseManager.getSongs(filters).size) {
                value
            } else {
                NO_CURRENT_SONG
            }
            positionObservable.onNext(field)
        }


    /**
     * Methods
     */
    fun getCurrentSong(realmInstance: Realm): Song = databaseManager.getSongs(filters, realmInstance)[listPosition]

    fun getCurrentAudioQueue(): RealmResults<Song> = databaseManager.getSongs(filters)
}