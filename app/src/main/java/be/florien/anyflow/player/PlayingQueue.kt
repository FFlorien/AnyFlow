package be.florien.anyflow.player

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbSongToPlay
import be.florien.anyflow.data.toDbSongToPlay
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
    var listPositionWithIntent: PositionWithIntent = PositionWithIntent(POSITION_NOT_SET, PlayingQueueIntent.PAUSE)
        get() {
            if (field.position == POSITION_NOT_SET) {
                listPositionWithIntent = PositionWithIntent(sharedPreferences.getInt(POSITION_PREF, 0), PlayingQueueIntent.PAUSE)
            }
            return field
        }
        set(value) {
            if (value.position <= queueSize - 1) {
                field = value
                MainScope().launch {
                    val currentSongId = songIdsListUpdater.value?.get(value.position) ?: DbSongToPlay(0L, null)
                    val nextSongId = songIdsListUpdater.value?.getOrNull(value.position + 1) ?: DbSongToPlay(0L, null)
                    (stateUpdater as MutableLiveData).value = PlayingQueueState(currentSongId, nextSongId, field.intent)
                    positionUpdater.value = field.position
                    orderComposer.currentPosition = field.position
                    sharedPreferences.applyPutInt(POSITION_PREF, field.position)
                    val songAtPosition = dataRepository.getSongAtPosition(field.position)
                    if (songAtPosition != this@PlayingQueue.currentSong.value) {
                        (this@PlayingQueue.currentSong as MutableLiveData).value = songAtPosition
                        orderComposer.currentSong = songAtPosition
                    }
                }
            }
        }
    var listPosition: Int
        get() {
            return listPositionWithIntent.position
        }
        set(value) {
            val previousIntent = listPositionWithIntent.intent
            listPositionWithIntent = PositionWithIntent(value, previousIntent)
        }

    var queueSize: Int = 0
    val positionUpdater = MutableLiveData<Int>()
    val currentSong: LiveData<SongInfo> = MutableLiveData()

    val songDisplayListUpdater: LiveData<PagingData<Song>> = dataRepository.getSongsInQueueOrder().cachedIn(CoroutineScope(Dispatchers.Default))
    private val songIdsListUpdater: LiveData<List<DbSongToPlay>> = dataRepository.getIdsInQueueOrder()
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
                it.indexOf(currentSong.value?.toDbSongToPlay())
            }
            listPosition = if (indexOf >= 0) indexOf else 0
        }
        GlobalScope.launch(Dispatchers.IO) {
            queueSize = dataRepository.getQueueSize() ?: 0
        }
    }

    class PlayingQueueState(val currentSong: DbSongToPlay, val nextSong: DbSongToPlay?, val intent: PlayingQueueIntent)

    enum class PlayingQueueIntent {
        START,
        PAUSE,
        CONTINUE
    }

    class PositionWithIntent(val position: Int, val intent: PlayingQueueIntent)
}