package be.florien.anyflow.player

import android.content.SharedPreferences
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Order
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.data.view.SongInfo
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
@Inject constructor(private val dataRepository: DataRepository, private val sharedPreferences: SharedPreferences, private val orderComposer: OrderComposer) {
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
            MainScope().launch {
                (stateUpdater as MutableLiveData).value = PlayingQueueState(songUrlListUpdater.value ?: listOf(), value)
                positionUpdater.value = field
                orderComposer.currentPosition = field
                sharedPreferences.applyPutInt(POSITION_PREF, field)
                val songAtPosition = dataRepository.getSongAtPosition(field)
                if (songAtPosition != currentSong.value) {
                    (currentSong as MutableLiveData).value = songAtPosition
                    orderComposer.currentSong = songAtPosition
                }
            }
            if (value != 0 && field == 0) {
                this@PlayingQueue.eLog(IllegalArgumentException("The new position may result from a faulty reset."))
            }
        }
    var queueSize: Int = 0
    val positionUpdater = MutableLiveData<Int>()
    val currentSong: LiveData<SongInfo> = MutableLiveData()

    val songDisplayListUpdater: LiveData<PagingData<Song>> = dataRepository.getSongsInQueueOrder().cachedIn(CoroutineScope(Dispatchers.Default))
    val songUrlListUpdater: LiveData<List<String>> = dataRepository.getUrlInQueueOrder()
    val stateUpdater: LiveData<PlayingQueueState> = MutableLiveData()
    val isOrderedUpdater: LiveData<Boolean> = dataRepository.getOrders()
            .map { orderList ->
                orderList.none { it.orderingType == Order.Ordering.RANDOM }
            }

    init {
        songUrlListUpdater.observeForever {
            queueSize = it.size

            val indexOf = if (currentSong.value == null) {
                listPosition
            } else {
                it.indexOf(currentSong.value?.url)
            }
            listPosition = if (indexOf >= 0) indexOf else 0
        }
        GlobalScope.launch(Dispatchers.IO) {
            queueSize = dataRepository.getQueueSize() ?: 0
        }
    }

    class PlayingQueueState(val urls: List<String>, val position: Int)
}