package be.florien.anyflow.management.queue

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.logging.eLog
import be.florien.anyflow.management.queue.model.Ordering
import be.florien.anyflow.management.queue.model.QueueItemDisplay
import be.florien.anyflow.tags.DataRepository
import be.florien.anyflow.tags.local.model.DbMediaToPlay
import be.florien.anyflow.tags.local.model.DbQueueItem
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.utils.applyPutInt
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

/**
 * Event handler for the queue of songs that are playing.
 */
@ServerScope
class PlayingQueue
@Inject constructor(
    private val queueRepository: QueueRepository,
    private val dataRepository: DataRepository,
    @Named("preferences") private val sharedPreferences: SharedPreferences,
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
                val mediaAtPosition = queueRepository.getMediaItemAtPosition(value)
                if (mediaAtPosition != this@PlayingQueue.currentMedia.value) {
                    (this@PlayingQueue.currentMedia as MutableLiveData).value = mediaAtPosition
                    withContext(Dispatchers.IO) {
                        if (mediaAtPosition?.mediaType == SONG_MEDIA_TYPE) {
                            orderComposer.currentSong = mediaAtPosition.let {
                                dataRepository.getSongSync(it.id)
                            }
                        }
                    }
                }
            }
        }

    val positionUpdater: LiveData<Int> = MutableLiveData(listPosition)
    val currentMedia: LiveData<DbQueueItem?> = MutableLiveData(null)

    val queueItemDisplayListUpdater: LiveData<PagingData<QueueItemDisplay>> =
        queueRepository.getQueueItems().cachedIn(coroutineScope)
    val mediaIdsListUpdater: Flow<List<DbMediaToPlay>> = queueRepository.getMediaIdsInQueueOrder().asFlow()
    val isOrderedUpdater: LiveData<Boolean> = queueRepository.getOrderings()
        .map { orderList ->
            orderList.none { it.orderingType == Ordering.OrderingType.RANDOM }
        }

    init {
        coroutineScope.launch {
            (currentMedia as MutableLiveData).postValue(queueRepository.getMediaItemAtPosition(listPosition))
        }
    }
}