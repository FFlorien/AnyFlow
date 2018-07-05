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

const val NO_CURRENT_SONG = -13456

/**
 * Manager for the queue of accounts that are playing. It handle filters, random, repeat and addition to the queue
 */
@UserScope
class AudioQueue
@Inject constructor(libraryDatabase: LibraryDatabase, private val sharedPreferences: SharedPreferences) {
    companion object {
        private const val POSITION_NOT_SET = -5
        private const val POSITION_PREF = "POSITION_PREF"
    }

    val positionUpdater: PublishSubject<Int> = PublishSubject.create()
    val currentSongUpdater: Flowable<Song?> = positionUpdater
            .toFlowable(BackpressureStrategy.LATEST)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnSubscribe {
                it.request(Long.MAX_VALUE)
            }
            .map { libraryDatabase.getSongAtPosition(it).firstOrNull() }

    val songListUpdater: Flowable<PagedList<SongDisplay>> = libraryDatabase.getSongsInQueueOrder()

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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    itemsCount = it.size
                }
    }
}