package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.persistence.SongsDatabase
import be.florien.ampacheplayer.persistence.model.QueueOrder
import be.florien.ampacheplayer.persistence.model.Song
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

const val NO_CURRENT_SONG = -13456

/**
 * Manager for the queue of accounts that are playing. It handle filters, random, repeat and addition to the queue
 */
@UserScope
class AudioQueue
@Inject constructor(private val songsDatabase: SongsDatabase) {

    /**
     * Fields
     */
    private var filters: MutableList<Filter<*>> = mutableListOf()
    val positionObservable: PublishSubject<Int> = PublishSubject.create()
    val orderObservable: PublishSubject<Boolean> = PublishSubject.create()
    val songList: MutableList<Song> = mutableListOf()
    val orderList: MutableList<QueueOrder> = mutableListOf()
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
        songsDatabase
                .getSongs()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    songList.clear()
                    songList.addAll(it)
                }
        songsDatabase
                .getQueueOrder()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    orderList.clear()
                    orderList.addAll(it)
                }
    }


    /**
     * Methods
     */
    fun getCurrentSong(): Song {
        val position = if (listPosition in 0 until orderList.size) orderList[listPosition].position else listPosition
        return if (listPosition in 0 until songList.size) songList[position] else Song()
    }

    fun setOrderRandom() {
        val orders = mutableListOf<Int>()
        for (order in 0 until songList.size) {
            orders.add(order)
        }

        orders.shuffle()
        val ordering = mutableListOf<QueueOrder>()

        for (position in 0 until songList.size) {
            ordering.add(QueueOrder(orders[position], position))
        }

        songsDatabase.setOrder(ordering)
        orderObservable.onNext(true)
    }//todo other than random

    fun getCurrentAudioQueue(): List<Song> = songList

    fun getCurrentQueueOrder(): List<QueueOrder> = orderList

    fun addFilter(filter: Filter<*>) = filters.add(filter)

    fun resetFilters() {
        filters.clear()
    }

}