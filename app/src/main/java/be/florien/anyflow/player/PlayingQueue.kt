package be.florien.anyflow.player

import android.arch.paging.PagedList
import android.content.SharedPreferences
import be.florien.anyflow.di.UserScope
import be.florien.anyflow.extension.applyPutInt
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.persistence.local.model.Song
import be.florien.anyflow.persistence.local.model.SongDisplay
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import javax.inject.Inject

/**
 * Event handler for the queue of songs that are playing.
 */
@UserScope
class PlayingQueue
@Inject constructor(private val libraryDatabase: LibraryDatabase, private val sharedPreferences: SharedPreferences) {
    companion object {
        private const val POSITION_NOT_SET = -5
        private const val POSITION_PREF = "POSITION_PREF"
    }

    var currentSong: Song? = null
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
    val positionUpdater: BehaviorSubject<Int> = BehaviorSubject.create()
    val currentSongUpdater: Flowable<Song?>
        get() = positionUpdater
                .flatMapMaybe { libraryDatabase.getSongAtPosition(it) }
                .toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged { song -> song.id }
                .subscribeOn(Schedulers.io())
                .share()
                .publish()
                .autoConnect()

    val songListUpdater: Flowable<PagedList<SongDisplay>> = libraryDatabase.getSongsInQueueOrder().replay(1).refCount()
    val orderingUpdater: Flowable<Boolean> =
            libraryDatabase
                    .getOrder()
                    .map { orderList ->
                        orderList.all { Order.toOrder(it).isRandom }
                    }

    val queueChangeUpdater: Flowable<Int> = libraryDatabase.changeUpdater.filter { it == LibraryDatabase.CHANGE_ORDER || it == LibraryDatabase.CHANGE_FILTERS }


    init {
        songListUpdater.doOnNext {
            itemsCount = it.size
            keepPositionCoherent()
        }.subscribe()

        currentSongUpdater
                .doOnNext {
                    currentSong = it
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()

        libraryDatabase.
                getPlaylist()
                .doOnNext {
                    val listToSave = if (libraryDatabase.randomOrderingSeed >= 0) {
                        val randomList = it.shuffled(Random(libraryDatabase.randomOrderingSeed.toLong())).toMutableList()
                        currentSong?.id?.let {currentSongId ->
                            if (randomList.remove(currentSongId)) {
                                randomList.add(0, currentSongId)
                            }
                        }
                        randomList
                    } else {
                        it.toMutableList()
                    }
                    libraryDatabase.saveOrder(listToSave)
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    private fun keepPositionCoherent() {
        val nullSafeSong = currentSong
        val single = if (nullSafeSong != null) {
            libraryDatabase.getPositionForSong(nullSafeSong)
        } else {
            Single.just(0)
        }
        single.onErrorReturnItem(0)
                .doOnSuccess {
                    listPosition = it
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()

    }
}