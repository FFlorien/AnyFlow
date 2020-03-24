package be.florien.anyflow.player

import android.content.SharedPreferences
import androidx.paging.PagedList
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.LibraryDatabase.Companion.CHANGE_FILTERS
import be.florien.anyflow.data.local.LibraryDatabase.Companion.CHANGE_ORDER
import be.florien.anyflow.data.view.Order
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.extension.applyPutInt
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.UserScope
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
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
@Inject constructor(private val dataRepository: DataRepository, private val sharedPreferences: SharedPreferences) {
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
            if (value != 0 && field == 0) {
                this@PlayingQueue.eLog(IllegalArgumentException("The new position may result from a faulty reset."))
            }
        }
    val positionUpdater: BehaviorSubject<Int> = BehaviorSubject.create()
    val currentSongUpdater: Flowable<Song?>
        get() = positionUpdater
                .flatMapMaybe { dataRepository.getSongAtPosition(it) }
                .toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged { song -> song.id }
                .subscribeOn(Schedulers.io())
                .share()
                .publish()
                .autoConnect()

    val songDisplayListUpdater: Flowable<PagedList<Song>> = dataRepository.getSongsInQueueOrder().replay(1).refCount()
    val isOrderedUpdater: Flowable<Boolean> =
            dataRepository
                    .getOrders()
                    .map { orderList ->
                        orderList.none { it.orderingType == Order.Ordering.RANDOM }
                    }

    val queueChangeUpdater: Flowable<Int> = dataRepository.changeUpdater.filter { it == CHANGE_ORDER || it == CHANGE_FILTERS }


    init {
        songDisplayListUpdater.doOnNext {
            itemsCount = it.size
            keepPositionCoherent()
        }.subscribe()

        currentSongUpdater
                .doOnNext {
                    currentSong = it
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()

        dataRepository.getOrderlessQueue()
                .doOnNext {
                    val listToSave = if (dataRepository.randomOrderingSeed >= 0) {
                        val randomList = it.shuffled(Random(dataRepository.randomOrderingSeed.toLong())).toMutableList()
                        dataRepository.precisePosition.forEach { preciseOrder ->
                            if (randomList.remove(preciseOrder.subject)) {
                                randomList.add(preciseOrder.argument, preciseOrder.subject)
                            }
                        }
                        randomList
                    } else {
                        it.toMutableList()
                    }
                    dataRepository.saveQueueOrder(listToSave)
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    private fun keepPositionCoherent() {
        val nullSafeSong = currentSong
        val maybe = if (nullSafeSong != null) {
            dataRepository.getPositionForSong(nullSafeSong)
        } else {
            Maybe.just(0)
        }
        maybe.doOnSuccess {
                    listPosition = it
                }
                .doOnComplete {
                    listPosition = 0
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()

    }
}