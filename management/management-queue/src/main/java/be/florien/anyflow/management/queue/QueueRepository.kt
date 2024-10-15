package be.florien.anyflow.management.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.room.withTransaction
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.tags.local.LibraryDatabase
import be.florien.anyflow.tags.local.query.QueryComposer
import be.florien.anyflow.tags.local.model.DbFilter
import be.florien.anyflow.tags.local.model.DbFilterGroup
import be.florien.anyflow.tags.local.model.DbQueueOrder
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.tags.toQueryFilters
import be.florien.anyflow.common.management.convertToPagingLiveData
import be.florien.anyflow.management.filters.FiltersRepository
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.FilterGroup
import be.florien.anyflow.management.queue.model.Ordering
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@ServerScope
class QueueRepository @Inject constructor(private val libraryDatabase: LibraryDatabase): FiltersRepository {

    private val queryComposer = QueryComposer()


    //region Filters

    suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Boolean =
        libraryDatabase.getPlaylistSongsDao().songInPlaylistCount(playlistId, songId) > 0

    suspend fun getMediaItemAtPosition(position: Int) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getQueueOrderDao().queueItemInPosition(position)
        }
    // endregion

    //region Current filters

    override fun getCurrentFilters(): LiveData<List<Filter<*>>> =
        libraryDatabase.getFilterDao().currentFiltersUpdatable().distinctUntilChanged()
            .map { filterList ->
                filterList.mapNotNull { filter ->
                    if (filter.parentFilter != null) {
                        null
                    } else {
                        filter.toViewFilter(filterList)
                    }
                }
            }

    override suspend fun setCurrentFilters(filters: List<Filter<*>>) {
        withContext(Dispatchers.IO) {
            //todo verify if first sync not showing anything at install isn't originating from here
            libraryDatabase.apply {
                withTransaction {
                    val currentFilters = getFilterDao().currentFilterList()
                    val currentFilterGroup = getFilterGroupDao().currentGroup()
                    updateHistory(currentFilters, currentFilterGroup)

                    insertCurrentFilterAndChildren(filters)
                    getFilterGroupDao().updateItems(currentFilterGroup.copy(dateAdded = Date().time))
                }
            }
        }
    }

    private suspend fun LibraryDatabase.updateHistory(
        currentFilters: List<DbFilter>, currentFilterGroup: DbFilterGroup
    ) {
        val history = getFilterGroupDao().historyGroupsList()
        if (history.size > HISTORY_SIZE) {
            getFilterGroupDao().deleteGroup(history.first().id)
        }

        val newHistoryItem = currentFilterGroup.copy(id = 0)
        val newId = getFilterGroupDao().insertItem(newHistoryItem)
        val newHistoryFilters = currentFilters.map { it.copy(filterGroup = newId) }
        getFilterDao().updateList(newHistoryFilters)
    }

    private suspend fun insertCurrentFilterAndChildren(
        filters: List<Filter<*>>,
        parentId: Long? = null
    ) {
        filters.forEach { filter ->
            val id = libraryDatabase.getFilterDao()
                .insertItem(filter.toDbFilter(DbFilterGroup.CURRENT_FILTER_GROUP_ID, parentId))
            insertCurrentFilterAndChildren(filter.children, id)
        }
    }
    //endregion

    //region FilterGroups

    override suspend fun saveFilterGroup(filters: List<Filter<*>>, name: String) =
        withContext(Dispatchers.IO) {
            if (libraryDatabase.getFilterGroupDao().filterGroupWithNameList(name).isEmpty()) {
                val filterGroup = DbFilterGroup(0, name, System.currentTimeMillis())
                val newId = libraryDatabase.getFilterGroupDao().insertItem(filterGroup)
                val filtersUpdated = filters.map { it.toDbFilter(newId) }
                libraryDatabase.getFilterDao().insertList(filtersUpdated)
            } else {
                throw IllegalArgumentException("A filter group with this name already exists")
            }
        }

    override fun getSavedGroups(): LiveData<List<FilterGroup>> = libraryDatabase.getFilterGroupDao().savedGroupUpdatable()
        .map { groupList -> groupList.map { it.toViewFilterGroup() } }

    override suspend fun setSavedGroupAsCurrentFilters(filterGroup: FilterGroup) {
        withContext(Dispatchers.IO) {
            libraryDatabase.apply {
                withTransaction {
                    val filterForGroup = getFilterDao().filtersForGroupList(filterGroup.id)
                    val currentFilters = getFilterDao().currentFilterList()
                    val currentFilterGroup = getFilterGroupDao().currentGroup()
                    updateHistory(currentFilters, currentFilterGroup)
                    getFilterDao().updateGroup(
                        currentFilterGroup,
                        filterForGroup.map { it.copy(id = null, filterGroup = 1) })
                }
            }
        }
    }
    //endregion

    //region Ordering

    fun getOrderings() =
        libraryDatabase.getOrderingDao().allUpdatable().distinctUntilChanged()
            .map { list -> list.map { item -> item.toViewOrdering() } }

    suspend fun setOrderings(orderings: List<Ordering>) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getOrderingDao().replaceBy(orderings.map { it.toDbOrdering() })
        }

    suspend fun saveQueueOrdering(listToSave: MutableList<QueueItem>) {
        libraryDatabase.getQueueOrderDao()
            .setOrder(listToSave.mapIndexed { index, id ->
                DbQueueOrder(index, id.id, id.mediaType)
            })
    }
    //endregion

    //region Queue

    fun getQueueItems() =
        libraryDatabase.getQueueOrderDao().displayInQueueOrderPaging()
            .map { it.toViewQueueItemDisplay() }
            .convertToPagingLiveData()

    fun getMediaIdsInQueueOrder() =
        libraryDatabase.getQueueOrderDao().mediaItemsInQueueOrderUpdatable()

    suspend fun getPositionForSong(songId: Long) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getQueueOrderDao().findPositionInQueue(songId)
        }

    suspend fun getOrderlessQueue(
        filterList: List<Filter<*>>,
        orderingList: List<Ordering>
    ): List<QueueItem> =
        withContext(Dispatchers.IO) {
            val songs = libraryDatabase.getSongDao().forCurrentFiltersList(
                queryComposer.getQueryForSongs(
                    filterList.toQueryFilters(),
                    orderingList.toQueryOrderings()
                )
            ).map {
                QueueItem(SONG_MEDIA_TYPE, it)
            }
            val podcastEpisodes = libraryDatabase.getPodcastEpisodeDao().rawQueryIdList(
                queryComposer.getQueryForPodcastEpisodes(filterList.toQueryFilters())
            ).map {
                QueueItem(PODCAST_MEDIA_TYPE, it)
            }
            podcastEpisodes + songs
        }
    //endregion

    data class QueueItem(val mediaType: Int, val id: Long)

    companion object {
        private const val HISTORY_SIZE = 100
    }
}