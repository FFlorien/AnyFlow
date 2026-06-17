package be.florien.anyflow.management.queue

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.paging.PagingData
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.logging.eLog
import be.florien.anyflow.common.utils.applyPutInt
import be.florien.anyflow.management.queue.model.Ordering
import be.florien.anyflow.tags.TagsRepository
import be.florien.anyflow.tags.local.model.DbMediaToPlay
import be.florien.anyflow.tags.local.model.DbQueueItem
import be.florien.anyflow.tags.local.model.DbQueueItemDisplay
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val tagsRepository: TagsRepository,
    @param:Named("preferences") private val sharedPreferences: SharedPreferences,
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
                (positionUpdater as MutableStateFlow).value = value
                orderComposer.currentPosition = value
                sharedPreferences.applyPutInt(POSITION_PREF, value)
                val mediaAtPosition = queueRepository.getMediaItemAtPosition(value)
                if (mediaAtPosition != this@PlayingQueue.currentMedia.value) {
                    (this@PlayingQueue.currentMedia as MutableLiveData).value = mediaAtPosition
                    withContext(Dispatchers.IO) {
                        if (mediaAtPosition?.mediaType == SONG_MEDIA_TYPE) {
                            orderComposer.currentSong = mediaAtPosition.let {
                                tagsRepository.getSongSync(it.id)
                            }
                        }
                    }
                }
            }
        }

    val positionUpdater: StateFlow<Int> = MutableStateFlow(listPosition)
    val currentMedia: LiveData<DbQueueItem?> = MutableLiveData(null)

    private var queueItemDisplayListUpdater: Flow<out PagingData<out Any>>? = null
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

    fun <T: Any> getQueueItemDisplayUpdater(mapping: (DbQueueItemDisplay) -> T): Flow<PagingData<T>> {
        val updater = queueItemDisplayListUpdater ?: queueRepository.getQueueItems(mapping)
        queueItemDisplayListUpdater = updater
        return updater as Flow<PagingData<T>>
    }
}