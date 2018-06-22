package be.florien.ampacheplayer.player

import android.arch.paging.PagedList
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.persistence.local.LocalDataManager
import be.florien.ampacheplayer.persistence.local.model.Song
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

const val NO_CURRENT_SONG = -13456

/**
 * Manager for the queue of accounts that are playing. It handle filters, random, repeat and addition to the queue
 */
@UserScope
class AudioQueue
@Inject constructor(localDataManager: LocalDataManager) {

    /**
     * Fields
     */
    val songListUpdater: Flowable<PagedList<Song>> = localDataManager.getSongsInQueueOrder()
    val positionObservable: PublishSubject<Int> = PublishSubject.create()
    val itemsCount: Int
        get() = currentAudioQueue?.size ?: 0
    var listPosition: Int = 0
        set(value) {
            field = when {
                value in 0 until itemsCount -> value
                value < 0 -> 0
                else -> itemsCount - 1
            }
            positionObservable.onNext(field)
        }

    var currentAudioQueue: PagedList<Song>? = null

    init {
        songListUpdater
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    currentAudioQueue = it
                }
    }


    /**
     * Methods
     */
    fun getCurrentSong(): Song {
        return if (itemsCount == 0) {
            Song()
        } else {
            currentAudioQueue?.get(listPosition) ?: Song()
        }
    }
}