package be.florien.ampacheplayer.player

import android.arch.paging.PagedList
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.persistence.local.LibraryDatabase
import be.florien.ampacheplayer.persistence.local.model.Song
import be.florien.ampacheplayer.persistence.local.model.SongDisplay
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

const val NO_CURRENT_SONG = -13456

/**
 * Manager for the queue of accounts that are playing. It handle filters, random, repeat and addition to the queue
 */
@UserScope
class AudioQueue
@Inject constructor(libraryDatabase: LibraryDatabase) {

    val positionUpdater: PublishSubject<Int> = PublishSubject.create()
    val currentSongUpdater: Flowable<Song?> = positionUpdater
            .toFlowable(BackpressureStrategy.LATEST)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { libraryDatabase.getSongAtPosition(it).firstOrNull() }

    val songListUpdater: Flowable<PagedList<SongDisplay>> = libraryDatabase.getSongsInQueueOrder()

    val itemsCount: Int
        get() = currentAudioQueue?.size ?: 0
    var currentAudioQueue: PagedList<SongDisplay>? = null
    var listPosition: Int = 0
        set(value) {
            field = when {
                value in 0 until itemsCount -> value
                value < 0 -> 0
                else -> itemsCount - 1
            }
            positionUpdater.onNext(field)
        }


    init {
        songListUpdater
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    currentAudioQueue = it
                }
    }
}