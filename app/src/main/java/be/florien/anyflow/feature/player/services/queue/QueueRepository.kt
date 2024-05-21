package be.florien.anyflow.feature.player.services.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.room.withTransaction
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.QueryComposer
import be.florien.anyflow.data.local.model.DbFilter
import be.florien.anyflow.data.local.model.DbFilterGroup
import be.florien.anyflow.data.local.model.DbQueueOrder
import be.florien.anyflow.data.toDbFilter
import be.florien.anyflow.data.toDbOrdering
import be.florien.anyflow.data.toViewFilter
import be.florien.anyflow.data.toViewFilterGroup
import be.florien.anyflow.data.toViewOrdering
import be.florien.anyflow.data.toViewSongDisplay
import be.florien.anyflow.data.toViewSongInfo
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.FilterGroup
import be.florien.anyflow.data.view.Ordering
import be.florien.anyflow.extension.convertToPagingLiveData
import be.florien.anyflow.injection.ServerScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@ServerScope
class QueueRepository @Inject constructor(private val libraryDatabase: LibraryDatabase) {

    private val queryComposer = QueryComposer()

    /**
     * Filters
     */

    suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Boolean =
        libraryDatabase.getPlaylistSongsDao().isPlaylistContainingSong(playlistId, songId) > 0

    suspend fun getSongAtPosition(position: Int) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getSongDao().forPositionInQueue(position)?.toViewSongInfo()
        }

    /**
     * Current filters
     */

    fun getCurrentFilters(): LiveData<List<Filter<*>>> =
        libraryDatabase.getFilterDao().currentFilters().distinctUntilChanged()
            .map { filterList ->
                filterList.mapNotNull { filter ->
                    if (filter.parentFilter != null) {
                        null
                    } else {
                        filter.toViewFilter(filterList)
                    }
                }
            }

    suspend fun setCurrentFilters(filterList: List<Filter<*>>) = withContext(Dispatchers.IO) {
        //todo verify if first sync not showing anything at install isn't originating from here
        libraryDatabase.apply {
            withTransaction {
                val currentFilters = getFilterDao().currentFiltersSync()
                val currentFilterGroup = getFilterGroupDao().currentSync()
                updateHistory(currentFilters, currentFilterGroup)

                insertCurrentFilterAndChildren(filterList)
                getFilterGroupDao().update(currentFilterGroup.copy(dateAdded = Date().time))
            }
        }
    }

    private suspend fun LibraryDatabase.updateHistory(
        currentFilters: List<DbFilter>, currentFilterGroup: DbFilterGroup
    ) {
        val history = getFilterGroupDao().historySync()
        if (history.size > HISTORY_SIZE) {
            getFilterGroupDao().deleteGroup(history.first().id)
        }

        val newHistoryItem = currentFilterGroup.copy(id = 0)
        val newId = getFilterGroupDao().insertSingle(newHistoryItem)
        val newHistoryFilters = currentFilters.map { it.copy(filterGroup = newId) }
        getFilterDao().updateAll(newHistoryFilters)
    }

    private suspend fun insertCurrentFilterAndChildren(
        filters: List<Filter<*>>,
        parentId: Long? = null
    ) {
        filters.forEach { filter ->
            val id = libraryDatabase.getFilterDao()
                .insertSingle(filter.toDbFilter(DbFilterGroup.CURRENT_FILTER_GROUP_ID, parentId))
            insertCurrentFilterAndChildren(filter.children, id)
        }
    }

    /**
     * FilterGroups
     */

    suspend fun saveFilterGroup(filterList: List<Filter<*>>, name: String) =
        withContext(Dispatchers.IO) {
            if (libraryDatabase.getFilterGroupDao().withNameIgnoreCase(name).isEmpty()) {
                val filterGroup = DbFilterGroup(0, name, System.currentTimeMillis())
                val newId = libraryDatabase.getFilterGroupDao().insertSingle(filterGroup)
                val filtersUpdated = filterList.map { it.toDbFilter(newId) }
                libraryDatabase.getFilterDao().insert(filtersUpdated)
            } else {
                throw IllegalArgumentException("A filter group with this name already exists")
            }
        }

    fun getSavedGroups() = libraryDatabase.getFilterGroupDao().saved()
        .map { groupList -> groupList.map { it.toViewFilterGroup() } }

    suspend fun setSavedGroupAsCurrentFilters(filterGroup: FilterGroup) =
        withContext(Dispatchers.IO) {
            libraryDatabase.apply {
                withTransaction {
                    val filterForGroup = getFilterDao().filterForGroup(filterGroup.id)
                    val currentFilters = getFilterDao().currentFiltersSync()
                    val currentFilterGroup = getFilterGroupDao().currentSync()
                    updateHistory(currentFilters, currentFilterGroup)
                    getFilterDao().updateGroup(
                        currentFilterGroup,
                        filterForGroup.map { it.copy(id = null, filterGroup = 1) })
                }
            }
        }

    /**
     * Ordering
     */

    fun getOrderings() =
        libraryDatabase.getOrderingDao().all().distinctUntilChanged()
            .map { list -> list.map { item -> item.toViewOrdering() } }

    suspend fun setOrderings(orderings: List<Ordering>) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getOrderingDao().replaceBy(orderings.map { it.toDbOrdering() })
        }

    suspend fun saveQueueOrdering(listToSave: MutableList<Long>) {
        libraryDatabase.getQueueOrderDao()
            .setOrder(listToSave.mapIndexed { index, id -> DbQueueOrder(index, id) })
    }

    /**
     * Queue
     */

    fun getSongsInQueueOrder() =
        libraryDatabase.getSongDao().displayInQueueOrder().map { it.toViewSongDisplay() }
            .convertToPagingLiveData()

    fun getIdsInQueueOrder() = libraryDatabase.getSongDao().songsInQueueOrder()

    suspend fun getPositionForSong(songId: Long) =
        withContext(Dispatchers.IO) { libraryDatabase.getSongDao().findPositionInQueue(songId) }

    suspend fun getOrderlessQueue(
        filterList: List<Filter<*>>,
        orderingList: List<Ordering>
    ): List<Long> =
        withContext(Dispatchers.IO) {
            libraryDatabase.getSongDao().forCurrentFilters(
                queryComposer.getQueryForSongs(filterList, orderingList)
            )
        }

    companion object {
        private const val HISTORY_SIZE = 100
        private const val SAVED_SIZE = 100
    }
}