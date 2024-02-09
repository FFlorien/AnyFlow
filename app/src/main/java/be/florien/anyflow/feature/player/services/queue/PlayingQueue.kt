package be.florien.anyflow.feature.player.services.queue

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.local.model.DbSongToPlay
import be.florien.anyflow.data.view.Order
import be.florien.anyflow.data.view.SongDisplay
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.applyPutInt
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ServerScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Event handler for the queue of songs that are playing.
 */
@ServerScope
class PlayingQueue
@Inject constructor(
    private val queueRepository: QueueRepository,
    private val sharedPreferences: SharedPreferences,
    private val orderComposer: OrderComposer
) {
    companion object {
        private const val POSITION_PREF = "POSITION_PREF"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        eLog(throwable, "Received an exception in playingQueue's scope")
    }
    private val coroutineScope = CoroutineScope(Dispatchers.Main + exceptionHandler)
    var listPosition: Int
        get() = sharedPreferences.getInt(POSITION_PREF, 0)
        set(value) {
            sharedPreferences.applyPutInt(POSITION_PREF, value)
            MainScope().launch {
                (positionUpdater as MutableLiveData).value = value
                orderComposer.currentPosition = value
                sharedPreferences.applyPutInt(POSITION_PREF, value)
                val songAtPosition = queueRepository.getSongAtPosition(value)
                if (songAtPosition != this@PlayingQueue.currentSong.value) {
                    (this@PlayingQueue.currentSong as MutableLiveData).value = songAtPosition
                    orderComposer.currentSong = songAtPosition
                }
            }
        }

    val positionUpdater: LiveData<Int> = MutableLiveData(listPosition)
    val currentSong: LiveData<SongInfo> = MutableLiveData()

    val songDisplayListUpdater: LiveData<PagingData<SongDisplay>> =
        queueRepository.getSongsInQueueOrder().cachedIn(coroutineScope)
    val songIdsListUpdater: Flow<List<DbSongToPlay>> = queueRepository.getIdsInQueueOrder().asFlow()
    val isOrderedUpdater: LiveData<Boolean> = queueRepository.getOrders()
        .map { orderList ->
            orderList.none { it.orderingType == Order.Ordering.RANDOM }
        }

    init {
        coroutineScope.launch {
            (currentSong as MutableLiveData).postValue(queueRepository.getSongAtPosition(listPosition))
        }
    }
}