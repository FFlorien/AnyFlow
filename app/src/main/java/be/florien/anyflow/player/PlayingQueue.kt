package be.florien.anyflow.player

import android.content.SharedPreferences
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.Order
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.extension.applyPutInt
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.UserScope
import kotlinx.coroutines.*
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

    var listPosition: Int = POSITION_NOT_SET
        get() {
            if (field == POSITION_NOT_SET) {
                listPosition = sharedPreferences.getInt(POSITION_PREF, 0)
            }
            return field
        }
        set(value) {
            field = value
            positionUpdater.value = field
            sharedPreferences.applyPutInt(POSITION_PREF, field)
            MainScope().launch {
                val songAtPosition = dataRepository.getSongAtPosition(field)
                if (songAtPosition != currentSong.value) {
                    (currentSong as MutableLiveData).value = songAtPosition
                }
            }
            if (value != 0 && field == 0) {
                this@PlayingQueue.eLog(IllegalArgumentException("The new position may result from a faulty reset."))
            }
        }
    var queueSize: Int = 0
    val positionUpdater = MutableLiveData<Int>()
    val currentSong: LiveData<Song> = MutableLiveData()

    val songDisplayListUpdater: LiveData<PagingData<Song>> = dataRepository.getSongsInQueueOrder().cachedIn(CoroutineScope(Dispatchers.Default))
    val songUrlListUpdater: LiveData<List<String>> = dataRepository.getUrlInQueueOrder()
    val isOrderedUpdater: LiveData<Boolean> = dataRepository.getOrders()
            .map { orderList ->
                orderList.none { it.orderingType == Order.Ordering.RANDOM }
            }
    private var randomOrderingSeed = 2
    private var precisePosition = listOf<Order>()

    init {
        songUrlListUpdater.observeForever {
            keepPositionCoherent()
        }
        val ordersLiveData = dataRepository.getOrders()
        val filtersLiveData = dataRepository.getCurrentFilters()

        ordersLiveData.observeForever {
            retrieveRandomness(it)
            val filterList = filtersLiveData.value
            if (filterList != null) {
                saveQueue(filterList, it)
            }
        }

        filtersLiveData.observeForever { filterList ->
            val orderList = ordersLiveData.value
            if (orderList != null) {
                saveQueue(filterList, orderList)
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            queueSize = dataRepository.getQueueSize() ?: 0
        }
    }

    private fun retrieveRandomness(orderList: List<Order>) {
        randomOrderingSeed = orderList
                .firstOrNull { it.orderingType == Order.Ordering.RANDOM }
                ?.argument ?: -1
        precisePosition = orderList.filter { it.orderingType == Order.Ordering.PRECISE_POSITION }
    }

    private fun saveQueue(filterList: List<Filter<*>>, it: List<Order>) {
        MainScope().launch {
            val queue = dataRepository.getOrderlessQueue(filterList, it)
            val listToSave = if (randomOrderingSeed >= 0) {
                val randomList = queue.shuffled(Random(randomOrderingSeed.toLong())).toMutableList()
                precisePosition.forEach { preciseOrder ->
                    if (randomList.remove(preciseOrder.subject)) {
                        randomList.add(preciseOrder.argument, preciseOrder.subject)
                    }
                }
                randomList
            } else {
                queue.toMutableList()
            }
            queueSize = listToSave.size
            dataRepository.saveQueueOrder(listToSave)
        }
    }

    private fun keepPositionCoherent() {
        MainScope().launch {
            val song = currentSong.value
            val newPosition = if (song != null) {
                dataRepository.getPositionForSong(song) ?: listPosition
            } else {
                listPosition
            }
            listPosition = newPosition
        }
    }
}