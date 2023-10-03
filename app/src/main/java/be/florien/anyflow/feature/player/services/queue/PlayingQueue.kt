package be.florien.anyflow.feature.player.services.queue

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.data.local.model.DbSongToPlay
import be.florien.anyflow.data.view.Order
import be.florien.anyflow.data.view.SongDisplay
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.applyPutInt
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.feature.player.services.WaveFormRepository
import be.florien.anyflow.injection.ServerScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
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
    private val orderComposer: OrderComposer,
    private val waveFormRepository: WaveFormRepository
) {
    companion object {
        private const val POSITION_NOT_SET = -5
        private const val POSITION_PREF = "POSITION_PREF"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        eLog(throwable, "Received an exception in playingQueue's scope")
    }
    private val coroutineScope = CoroutineScope(Dispatchers.Main + exceptionHandler)
    var listPositionWithIntent = PositionWithIntent(POSITION_NOT_SET, PlayingQueueIntent.PAUSE)
        get() {
            if (field.position == POSITION_NOT_SET) {
                listPositionWithIntent = PositionWithIntent(
                    sharedPreferences.getInt(POSITION_PREF, 0),
                    PlayingQueueIntent.PAUSE
                )
            }
            return field
        }
        set(value) {
            if (value.position <= queueSize - 1) {
                field = value
                MainScope().launch {
                    val currentSongId = songIdsListUpdater.value?.get(value.position)
                        ?: DbSongToPlay(0L, null)
                    val nextSongId = songIdsListUpdater.value?.getOrNull(value.position + 1)
                        ?: DbSongToPlay(0L, null)
                    (stateUpdater as MutableLiveData).value =
                        PlayingQueueState(currentSongId, nextSongId, field.intent)
                    positionUpdater.value = field.position
                    orderComposer.currentPosition = field.position
                    sharedPreferences.applyPutInt(POSITION_PREF, field.position)
                    waveFormRepository.checkWaveForm(currentSongId.id)
                    waveFormRepository.checkWaveForm(nextSongId.id)
                    val songAtPosition = queueRepository.getSongAtPosition(field.position)
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

    val songDisplayListUpdater: LiveData<PagingData<SongDisplay>> =
        queueRepository.getSongsInQueueOrder().cachedIn(coroutineScope)
    private val songIdsListUpdater: LiveData<List<DbSongToPlay>> =
        queueRepository.getIdsInQueueOrder()
    val stateUpdater: LiveData<PlayingQueueState> = MutableLiveData()
    val isOrderedUpdater: LiveData<Boolean> = queueRepository.getOrders()
        .map { orderList ->
            orderList.none { it.orderingType == Order.Ordering.RANDOM }
        }

    init {
        songIdsListUpdater.observeForever { songToPlayList ->
            queueSize = songToPlayList.size

            val indexOf = if (currentSong.value == null) {
                listPosition
            } else {
                songToPlayList.indexOfFirst { it.id == currentSong.value?.id }
            }
            listPosition = if (indexOf >= 0) indexOf else 0
        }
        coroutineScope.launch(Dispatchers.IO) {
            queueSize = queueRepository.getQueueSize() ?: 0
        }
    }

    class PlayingQueueState(
        val currentSong: DbSongToPlay,
        val nextSong: DbSongToPlay?,
        val intent: PlayingQueueIntent
    )

    enum class PlayingQueueIntent {
        START,
        PAUSE,
        CONTINUE
    }

    class PositionWithIntent(val position: Int, val intent: PlayingQueueIntent)
}