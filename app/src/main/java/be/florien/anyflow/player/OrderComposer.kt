package be.florien.anyflow.player

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.Order
import be.florien.anyflow.data.view.Order.Companion.RANDOM_MULTIPLIER
import be.florien.anyflow.data.view.SongInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderComposer @Inject constructor(private val dataRepository: DataRepository) {
    private var currentOrders = listOf<Order>()
    var currentSong: SongInfo? = null
    var currentPosition: Int = -1
    var areFirstFilterArrived = false

    init {
        val ordersLiveData = dataRepository.getOrders()
        val filtersLiveData = dataRepository.getCurrentFilters()

        ordersLiveData.observeForever {
            currentOrders = it
            val filterList = filtersLiveData.value
            if (filterList != null) {
                saveQueue(filterList, currentOrders)
            }
        }

        filtersLiveData.observeForever { filterList ->
            CoroutineScope(Dispatchers.IO).launch {
                val newOrders = if (areFirstFilterArrived) {
                    currentOrders.filter { it.orderingType != Order.Ordering.PRECISE_POSITION }.toMutableList()
                } else {
                    currentOrders.toMutableList()
                }
                areFirstFilterArrived = true
                if (newOrders.any { it.orderingType == Order.Ordering.RANDOM }) {
                    currentSong?.let { songInfo ->
                        val isCurrentSongInNewFilters = filterList.any { it.contains(songInfo, dataRepository) }
                        if (isCurrentSongInNewFilters) {
                            newOrders.add(Order.Precise(0, songId = songInfo.id, priority = Order.PRIORITY_PRECISE))
                        }
                    }
                }
                if (newOrders.containsAll(currentOrders) && currentOrders.containsAll(newOrders)) {
                    saveQueue(filterList, newOrders)
                } else {
                    dataRepository.setOrders(newOrders)
                }
            }
        }
    }

    suspend fun changeSongPositionForNext(songId: Long) {
        if (currentSong?.id == songId) {
            return
        }

        val newOrders = currentOrders.toMutableList()
        val songPosition = dataRepository.getPositionForSong(songId)

        val newPosition = if (songPosition != null && songPosition < currentPosition) {
            currentPosition
        } else {
            currentPosition + 1
        }

        val lastPrecise = newOrders.lastOrNull { it.orderingType == Order.Ordering.PRECISE_POSITION && it.argument == newPosition }
        val newPrecise = Order.Precise(precisePosition = newPosition, songId = songId, priority = lastPrecise?.priority?.plus(1) ?: Order.PRIORITY_PRECISE)
        newOrders.add(newPrecise)
        dataRepository.setOrders(newOrders)

    }

    suspend fun randomize() {
        val orders = mutableListOf<Order>(Order.Random(0, Order.SUBJECT_ALL, Math.random().times(RANDOM_MULTIPLIER).toInt()))
        currentSong?.let { song ->
            orders.add(Order.Precise(0, song.id, Order.PRIORITY_PRECISE))
        }
        dataRepository.setOrders(orders)
    }

    suspend fun order() {
        val dbOrders = listOf(Order.SUBJECT_ALBUM_ARTIST, Order.SUBJECT_YEAR, Order.SUBJECT_ALBUM, Order.SUBJECT_TRACK, Order.SUBJECT_TITLE)
                .mapIndexed { index, order ->
                    Order.Ordered(
                            index,
                            order
                    )
                }
        dataRepository.setOrders(dbOrders)
    }

    private fun saveQueue(filterList: List<Filter<*>>, orderList: List<Order>) {
        CoroutineScope(Dispatchers.IO).launch {
            val queue = dataRepository.getOrderlessQueue(filterList, orderList)
            val randomOrderingSeed = orderList
                    .firstOrNull { it.orderingType == Order.Ordering.RANDOM }
                    ?.argument
            val listToSave = if (randomOrderingSeed != null) {
                queue.shuffled(Random(randomOrderingSeed.toLong())).toMutableList()
            } else {
                queue.toMutableList()
            }
            currentOrders.filter { it.orderingType == Order.Ordering.PRECISE_POSITION }.forEach { preciseOrder ->
                if (listToSave.remove(preciseOrder.subject)) {
                    listToSave.add(preciseOrder.argument, preciseOrder.subject)
                } // todo else remove order from db
            }
            dataRepository.saveQueueOrder(listToSave)
        }
    }
}