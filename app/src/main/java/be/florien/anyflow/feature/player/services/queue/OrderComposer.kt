package be.florien.anyflow.feature.player.services.queue

import be.florien.anyflow.data.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.Ordering
import be.florien.anyflow.data.view.Ordering.Companion.RANDOM_MULTIPLIER
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.injection.ServerScope
import be.florien.anyflow.logging.eLog
import be.florien.anyflow.logging.iLog
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
    private var currentOrderings = listOf<Ordering>()
    var currentSong: SongInfo? = null
    var currentPosition: Int = -1
    private var areFirstFiltersArrived = false
    private var areFirstOrdersArrived = false

    init {
        val ordersLiveData = queueRepository.getOrderings()
        val filtersLiveData = queueRepository.getCurrentFilters()

        ordersLiveData.observeForever {
            iLog("We will maybe save queue order form ordersLiveData.observeForever")
            areFirstOrdersArrived = true
            currentOrderings = it
            val filterList = filtersLiveData.value
            if (areFirstFiltersArrived && filterList != null) {
                saveQueue(filterList, currentOrderings)
            }
        }

        filtersLiveData.observeForever { filterList ->
            coroutineScope.launch(Dispatchers.IO) {
                iLog("We will maybe save queue order form filtersLiveData.observeForever")
                if (!areFirstOrdersArrived) {
                    areFirstFiltersArrived = true
                    return@launch
                }
                val newOrderings =
                    if (areFirstFiltersArrived) { // todo this code is for avoiding current song at first position multiple time, but cause bug with play next
                        currentOrderings
                            .filter { it.orderingType != Ordering.OrderingType.PRECISE_POSITION }
                            .toMutableList()
                    } else {
                        currentOrderings.toMutableList()
                    }
                areFirstFiltersArrived = true

                if (newOrderings.any { it.orderingType == Ordering.OrderingType.RANDOM }) {
                    getCurrentSongPrecisePositionIfPresent(filterList)?.let { newOrderings.add(it) }
                }
                if (newOrderings.containsAll(currentOrderings) && currentOrderings.containsAll(
                        newOrderings
                    )
                ) {
                    saveQueue(filterList, newOrderings)
                } else {
                    queueRepository.setOrderings(newOrderings)
                }
            }
        }
    }

    private suspend fun getCurrentSongPrecisePositionIfPresent(filterList: List<Filter<*>>): Ordering.Precise? {
        currentSong?.let { songInfo ->
            val isCurrentSongInNewFilters =
                filterList.any { it.contains(songInfo, queueRepository) }
            if (isCurrentSongInNewFilters) {
                return Ordering.Precise(
                    0,
                    songId = songInfo.id,
                    priority = Ordering.PRIORITY_PRECISE
                )
            }
        }
        return null
    }

    suspend fun changeSongPositionForNext(songId: Long) {
        if (currentSong?.id == songId) {
            return
        }

        val newOrders = currentOrderings.toMutableList()
        val songPosition = queueRepository.getPositionForSong(songId)

        val newPosition = if (songPosition != null && songPosition < currentPosition) {
            currentPosition
        } else {
            currentPosition + 1
        }

        val lastPrecise =
            newOrders.lastOrNull { it.orderingType == Ordering.OrderingType.PRECISE_POSITION && it.argument == newPosition }
        val newPrecise = Ordering.Precise(
            precisePosition = newPosition,
            songId = songId,
            priority = lastPrecise?.priority?.plus(1) ?: Ordering.PRIORITY_PRECISE
        )
        newOrders.add(newPrecise)
        queueRepository.setOrderings(newOrders)

    }

    suspend fun randomize() {
        val orderings = mutableListOf<Ordering>(
            Ordering.Random(
                0,
                Ordering.SUBJECT_ALL,
                Math.random().times(RANDOM_MULTIPLIER).toInt()
            )
        )
        currentSong?.let { song ->
            orderings.add(Ordering.Precise(0, song.id, Ordering.PRIORITY_PRECISE))
        }
        queueRepository.setOrderings(orderings)
    }

    suspend fun order() {
        val dbOrders = listOf(
            Ordering.SUBJECT_ALBUM_ARTIST,
            Ordering.SUBJECT_YEAR,
            Ordering.SUBJECT_ALBUM,
            Ordering.SUBJECT_DISC,
            Ordering.SUBJECT_TRACK,
            Ordering.SUBJECT_TITLE
        ).mapIndexed { index, order ->
            Ordering.Ordered(
                index,
                order
            )
        }
        queueRepository.setOrderings(dbOrders)
    }

    private fun saveQueue(filterList: List<Filter<*>>, orderingList: List<Ordering>) {
        iLog("Order for saving queue order: ${orderingList.joinToString { it.orderingSubject.name }}")
        coroutineScope.launch {
            val queue = queueRepository.getOrderlessQueue(filterList, orderingList)
            val randomOrderingSeed = orderingList
                .firstOrNull { it.orderingType == Ordering.OrderingType.RANDOM }
                ?.argument
            val listToSave = if (randomOrderingSeed != null) {
                queue.shuffled(Random(randomOrderingSeed.toLong())).toMutableList()
            } else {
                queue.toMutableList()
            }
            currentOrderings
                .filter { it.orderingType == Ordering.OrderingType.PRECISE_POSITION }
                .forEach { preciseOrder ->
                    if (listToSave.remove(
                            QueueRepository.QueueItem(
                                SONG_MEDIA_TYPE,
                                preciseOrder.subject
                            )
                        )
                    ) { //todo change podcast position
                        listToSave.add(
                            preciseOrder.argument,
                            QueueRepository.QueueItem(SONG_MEDIA_TYPE, preciseOrder.subject)
                        )
                    } // todo else remove order from db
                }
            queueRepository.saveQueueOrdering(listToSave)
        }
    }
}