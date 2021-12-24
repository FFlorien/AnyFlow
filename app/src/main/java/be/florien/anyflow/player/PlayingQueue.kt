package be.florien.anyflow.player

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Order
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.applyPutInt
import be.florien.anyflow.injection.UserScope
import kotlinx.coroutines.*
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
            if (value <= queueSize - 1) {
                field = value
                MainScope().launch {
                    (stateUpdater as MutableLiveData).value = PlayingQueueState(songIdsListUpdater.value ?: listOf(), value)
                    positionUpdater.value = field
                    orderComposer.currentPosition = field
                    sharedPreferences.applyPutInt(POSITION_PREF, field)
                    val songAtPosition = dataRepository.getSongAtPosition(field)
                    if (songAtPosition != currentSong.value) {
                        (currentSong as MutableLiveData).value = songAtPosition
                        orderComposer.currentSong = songAtPosition
                    }
                }
            }
        }
    var queueSize: Int = 0
    val positionUpdater = MutableLiveData<Int>()
    val currentSong: LiveData<SongInfo> = MutableLiveData()

    val songDisplayListUpdater: LiveData<PagingData<Song>> = dataRepository.getSongsInQueueOrder().cachedIn(CoroutineScope(Dispatchers.Default))
    private val songIdsListUpdater: LiveData<List<Long>> = dataRepository.getIdsInQueueOrder()
    val stateUpdater: LiveData<PlayingQueueState> = MutableLiveData()
    val isOrderedUpdater: LiveData<Boolean> = dataRepository.getOrders()
        .map { orderList ->
            orderList.none { it.orderingType == Order.Ordering.RANDOM }
        }

    init {
        songIdsListUpdater.observeForever {
            queueSize = it.size

            val indexOf = if (currentSong.value == null) {
                listPosition
            } else {
                it.indexOf(currentSong.value?.id)
            }
            listPosition = if (indexOf >= 0) indexOf else 0
        }
        GlobalScope.launch(Dispatchers.IO) {
            queueSize = dataRepository.getQueueSize() ?: 0
        }
    }

    class PlayingQueueState(val ids: List<Long>, val position: Int)
}