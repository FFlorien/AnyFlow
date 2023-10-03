package be.florien.anyflow.feature.player.services.queue

import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.Order
import be.florien.anyflow.data.view.Order.Companion.RANDOM_MULTIPLIER
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ServerScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random
import javax.inject.Inject

@ServerScope
class OrderComposer @Inject constructor(private val queueRepository: QueueRepository) {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        eLog(throwable, "Received an exception in OrderComposer's scope")
    }
    private val coroutineScope = CoroutineScope(Dispatchers.Main + exceptionHandler)
    private var currentOrders = listOf<Order>()
    var currentSong: SongInfo? = null
    var currentPosition: Int = -1
    private var areFirstFiltersArrived = false
    private var areFirstOrdersArrived = false

    init {
        val ordersLiveData = queueRepository.getOrders()
        val filtersLiveData = queueRepository.getCurrentFilters()

        ordersLiveData.observeForever {
            FirebaseCrashlytics.getInstance()
                .log("We will maybe save queue order form ordersLiveData.observeForever")
            areFirstOrdersArrived = true
            currentOrders = it
            val filterList = filtersLiveData.value
            if (areFirstFiltersArrived && filterList != null) {
                saveQueue(filterList, currentOrders)
            }
        }

        filtersLiveData.observeForever { filterList ->
            coroutineScope.launch(Dispatchers.IO) {
                FirebaseCrashlytics
                    .getInstance()
                    .log("We will maybe save queue order form filtersLiveData.observeForever")
                if (!areFirstOrdersArrived) {
                    areFirstFiltersArrived = true
                    return@launch
                }
                val newOrders =
                    if (areFirstFiltersArrived) { // todo this code is for avoiding current song at first position multiple time, but cause bug with play next
                        currentOrders
                            .filter { it.orderingType != Order.Ordering.PRECISE_POSITION }
                            .toMutableList()
                    } else {
                        currentOrders.toMutableList()
                    }
                areFirstFiltersArrived = true

                if (newOrders.any { it.orderingType == Order.Ordering.RANDOM }) {
                    getCurrentSongPrecisePositionIfPresent(filterList)?.let { newOrders.add(it) }
                }
                if (newOrders.containsAll(currentOrders) && currentOrders.containsAll(newOrders)) {
                    saveQueue(filterList, newOrders)
                } else {
                    queueRepository.setOrders(newOrders)
                }
            }
        }
    }

    private suspend fun getCurrentSongPrecisePositionIfPresent(filterList: List<Filter<*>>): Order.Precise? {
        currentSong?.let { songInfo ->
            val isCurrentSongInNewFilters =
                filterList.any { it.contains(songInfo, queueRepository) }
            if (isCurrentSongInNewFilters) {
                return Order.Precise(0, songId = songInfo.id, priority = Order.PRIORITY_PRECISE)
            }
        }
        return null
    }

    suspend fun changeSongPositionForNext(songId: Long) {
        if (currentSong?.id == songId) {
            return
        }

        val newOrders = currentOrders.toMutableList()
        val songPosition = queueRepository.getPositionForSong(songId)

        val newPosition = if (songPosition != null && songPosition < currentPosition) {
            currentPosition
        } else {
            currentPosition + 1
        }

        val lastPrecise =
            newOrders.lastOrNull { it.orderingType == Order.Ordering.PRECISE_POSITION && it.argument == newPosition }
        val newPrecise = Order.Precise(
            precisePosition = newPosition,
            songId = songId,
            priority = lastPrecise?.priority?.plus(1) ?: Order.PRIORITY_PRECISE
        )
        newOrders.add(newPrecise)
        queueRepository.setOrders(newOrders)

    }

    suspend fun randomize() {
        val orders = mutableListOf<Order>(
            Order.Random(
                0,
                Order.SUBJECT_ALL,
                Math.random().times(RANDOM_MULTIPLIER).toInt()
            )
        )
        currentSong?.let { song ->
            orders.add(Order.Precise(0, song.id, Order.PRIORITY_PRECISE))
        }
        queueRepository.setOrders(orders)
    }

    suspend fun order() {
        val dbOrders = listOf(
            Order.SUBJECT_ALBUM_ARTIST,
            Order.SUBJECT_YEAR,
            Order.SUBJECT_ALBUM,
            Order.SUBJECT_ALBUM_ID,
            Order.SUBJECT_TRACK,
            Order.SUBJECT_TITLE
        ).mapIndexed { index, order ->
            Order.Ordered(
                index,
                order
            )
        }
        queueRepository.setOrders(dbOrders)
    }

    private fun saveQueue(filterList: List<Filter<*>>, orderList: List<Order>) {
        FirebaseCrashlytics.getInstance()
            .log("Order for saving queue order: ${orderList.joinToString { it.orderingSubject.name }}")
        coroutineScope.launch {
            val queue = queueRepository.getOrderlessQueue(filterList, orderList)
            val randomOrderingSeed = orderList
                .firstOrNull { it.orderingType == Order.Ordering.RANDOM }
                ?.argument
            val listToSave = if (randomOrderingSeed != null) {
                queue.shuffled(Random(randomOrderingSeed.toLong())).toMutableList()
            } else {
                queue.toMutableList()
            }
            currentOrders
                .filter { it.orderingType == Order.Ordering.PRECISE_POSITION }
                .forEach { preciseOrder ->
                    if (listToSave.remove(preciseOrder.subject)) {
                        listToSave.add(preciseOrder.argument, preciseOrder.subject)
                    } // todo else remove order from db
                }
            queueRepository.saveQueueOrder(listToSave)
        }
    }
}