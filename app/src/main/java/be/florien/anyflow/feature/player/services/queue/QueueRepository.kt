package be.florien.anyflow.feature.player.services.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.room.withTransaction
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.QueryComposer
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
        libraryDatabase.apply {
            withTransaction {
                getFilterDao().deleteGroupSync(DbFilterGroup.CURRENT_FILTER_GROUP_ID)
                insertCurrentFilterAndChildren(filterList)
            }
        }
    }

    private suspend fun insertCurrentFilterAndChildren(
        filters: List<Filter<*>>,
        parentId: Long? = null
    ) {
        filters.forEach { filter ->
            val id = libraryDatabase.getFilterDao().insertSingle(filter.toDbFilter(1, parentId))
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

    fun getFilterGroups() = libraryDatabase.getFilterGroupDao().allFromRange(SAVED_START_INDEX_INCL, SAVED_END_INDEX_EXCL + 1)
        .map { groupList -> groupList.map { it.toViewFilterGroup() } }

    suspend fun setSavedGroupAsCurrentFilters(filterGroup: FilterGroup) =
        withContext(Dispatchers.IO) {
            libraryDatabase.apply {
                withTransaction {
                    val filterForGroup = getFilterDao().filterForGroup(filterGroup.id)
                    getFilterDao().updateGroup(
                        DbFilterGroup.currentFilterGroup,
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

    suspend fun getOrderlessQueue(filterList: List<Filter<*>>, orderingList: List<Ordering>): List<Long> =
        withContext(Dispatchers.IO) {
            libraryDatabase.getSongDao().forCurrentFilters(
                queryComposer.getQueryForSongs(filterList, orderingList)
            )
        }
}