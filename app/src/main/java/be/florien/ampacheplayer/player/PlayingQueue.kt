package be.florien.ampacheplayer.player

import android.arch.paging.PagedList
import android.content.SharedPreferences
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.extension.applyPutInt
import be.florien.ampacheplayer.persistence.local.LibraryDatabase
import be.florien.ampacheplayer.persistence.local.model.Song
import be.florien.ampacheplayer.persistence.local.model.SongDisplay
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Event handler for the queue of songs that are playing.
 */
@UserScope
class PlayingQueue
@Inject constructor(libraryDatabase: LibraryDatabase, private val sharedPreferences: SharedPreferences) {
    companion object {
        private const val POSITION_NOT_SET = -5
        private const val POSITION_PREF = "POSITION_PREF"
    }

    val positionUpdater: PublishSubject<Int> = PublishSubject.create()
    val currentSongUpdater: Flowable<Song?> = positionUpdater
            .toFlowable(BackpressureStrategy.LATEST)
            .flatMap { libraryDatabase.getSongAtPosition(it) }
            .distinctUntilChanged { song -> song.id }
            .subscribeOn(Schedulers.io())
            .share()

    val songListUpdater: Flowable<PagedList<SongDisplay>> = libraryDatabase.getSongsInQueueOrder().share()

    var itemsCount: Int = 0
    var listPosition: Int = POSITION_NOT_SET
        get() {
            if (field == POSITION_NOT_SET) {
                field = sharedPreferences.getInt(POSITION_PREF, 0)
                positionUpdater.onNext(field)
            }
            return field
        }
        set(value) {
            field = when {
                value in 0 until itemsCount -> value
                value < 0 -> 0
                else -> itemsCount - 1
            }
            positionUpdater.onNext(field)
            sharedPreferences.applyPutInt(POSITION_PREF, field)
        }


    init {
        songListUpdater
                .doOnNext {
                    itemsCount = it.size
                }
                .flatMap {
                    libraryDatabase.getSongAtPosition(listPosition)
                }
                .distinct { it.id }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    listPosition = 0
                }
    }
}