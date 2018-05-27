package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.persistence.SongsDatabase
import be.florien.ampacheplayer.persistence.model.QueueOrder
import be.florien.ampacheplayer.persistence.model.Song
import io.reactivex.subjects.PublishSubject
import io.realm.RealmResults
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
    val itemsCount: Int
        get() = songsDatabase.getSongs(filters).size
    var listPosition: Int = 0
        set(value) {
            field = when {
                value in 0 until songsDatabase.getSongs(filters).size -> value
                value < 0 -> 0
                else -> songsDatabase.getSongs(filters).size -1
            }
            positionObservable.onNext(field)
        }


    /**
     * Methods
     */
    fun getCurrentSong(): Song {
        val songs = songsDatabase.getSongs(filters)
        val order = songsDatabase.getQueueOrder()
        val position = if (listPosition in 0 until order.size) order[listPosition]?.position ?: listPosition else listPosition
        return if (listPosition in 0 until songs.size) songs[position] ?: Song() else Song()
    }

    fun setOrderRandom() {
        val songList = songsDatabase.getSongs()
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

    fun getCurrentAudioQueue(): RealmResults<Song> = songsDatabase.getSongs(filters)

    fun getCurrentQueueOrder(): RealmResults<QueueOrder> = songsDatabase.getQueueOrder()

    fun addFilter(filter: Filter<*>) = filters.add(filter)

    fun resetFilters() {filters.clear()}

}