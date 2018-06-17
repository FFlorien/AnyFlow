package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.persistence.PersistenceManager
import be.florien.ampacheplayer.persistence.local.model.Filter
import be.florien.ampacheplayer.persistence.local.model.Song
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

const val NO_CURRENT_SONG = -13456

/**
 * Manager for the queue of accounts that are playing. It handle filters, random, repeat and addition to the queue
 */
@UserScope
class AudioQueue
@Inject constructor(persistenceManager: PersistenceManager) {

    /**
     * Fields
     */
    val positionObservable: PublishSubject<Int> = PublishSubject.create()
    val songList: MutableList<Song> = mutableListOf()
    val itemsCount: Int
        get() = songList.size
    var listPosition: Int = 0
        set(value) {
            field = when {
                value in 0 until songList.size -> value
                value < 0 -> 0
                else -> songList.size - 1
            }
            positionObservable.onNext(field)
        }

    init {
        persistenceManager
                .getSongsInQueueOrder()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    songList.clear()
                    songList.addAll(it)
                }
    }


    /**
     * Methods
     */
    fun getCurrentSong(): Song {
        return if (itemsCount == 0) {
            Song()
        } else {
            songList[listPosition]
        }
    }

    fun getCurrentAudioQueue(): List<Song> = songList
}