package be.florien.anyflow.management.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.paging.PagingData
import androidx.room.withTransaction
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.management.convertToPagingFlow
import be.florien.anyflow.management.filters.domain.FiltersRepository
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterGroup
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import be.florien.anyflow.management.queue.model.Ordering
import be.florien.anyflow.tags.local.LibraryDatabase
import be.florien.anyflow.tags.local.model.DbFilter
import be.florien.anyflow.tags.local.model.DbFilterGroup
import be.florien.anyflow.tags.local.model.DbQueueItemDisplay
import be.florien.anyflow.tags.local.model.DbQueueOrder
import be.florien.anyflow.tags.local.model.PODCAST_CHAPTER_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.tags.local.query.QueryComposer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@ServerScope
class QueueRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val queryComposer: QueryComposer
) : FiltersRepository {
    //region Filters

    suspend fun isPlaylistContainingSong(playlistId: Long, songId: Long): Boolean =
        libraryDatabase.getPlaylistSongsDao().songInPlaylistCount(playlistId, songId) > 0

    suspend fun getMediaItemAtPosition(position: Int) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getQueueOrderDao().queueItemInPosition(position)
        }
    // endregion

    //region Current filters

    override fun getCurrentFilters(): LiveData<List<Filter>> =
        libraryDatabase.getFilterDao().currentFiltersUpdatable().distinctUntilChanged()
            .map {
                it.toViewFilters()
            }

    override suspend fun setCurrentFilters(filters: List<Filter>) {
        withContext(Dispatchers.IO) {
            //todo verify if first sync not showing anything at install isn't originating from here
            libraryDatabase.apply {
                withTransaction {
                    val currentFilters = getFilterDao().currentFilterList()
                    val currentFilterGroup = getFilterGroupDao().currentGroup()
                    updateHistory(currentFilters, currentFilterGroup)

                    insertFilters(filters, DbFilterGroup.CURRENT_FILTER_GROUP_ID)
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
        getFilterGroupDao().updateItems(currentFilterGroup.copy(dateAdded = Date().time))
    }

    private suspend fun insertFilters(filters: List<Filter>, groupId: Long) {
        filters.forEach { filter ->
            var parentId: Long? = null
            filter.forEach {
                parentId = libraryDatabase
                    .getFilterDao()
                    .insertItem(it.toDbFilter(groupId, parentId))
            }
        }
    }
    //endregion

    //region FilterGroups

    override suspend fun saveFilterGroup(filters: List<Filter>, name: String) {
        withContext(Dispatchers.IO) {
            if (libraryDatabase.getFilterGroupDao().filterGroupWithNameList(name).isEmpty()) {
                val filterGroup = DbFilterGroup(0, name, System.currentTimeMillis())
                val newId = libraryDatabase.getFilterGroupDao().insertItem(filterGroup)
                insertFilters(filters, newId)
            } else {
                throw IllegalArgumentException("A filter group with this name already exists")
            }
        }
    }

    override fun getHistoryAndFilterGroups(): Flow<List<FilterGroup>> =
        libraryDatabase.getFilterGroupDao().savedGroupUpdatable()
            .zip(libraryDatabase.getFilterGroupDao().historyGroupsUpdatable()) { saved, history ->
                listOf(*(saved.toTypedArray()), *(history.toTypedArray()))
            }
            .map { groupList -> groupList.map { it.toViewFilterGroup() } }

    override suspend fun setSavedGroupAsCurrentFilters(filterGroupId: Long) {
        withContext(Dispatchers.IO) {
            libraryDatabase.apply {
                withTransaction {
                    val filterForGroup = getFilterDao().filtersForGroupList(filterGroupId)
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

    suspend fun getOrderingsSuspend() =
        libraryDatabase.getOrderingDao().allList().map { item -> item.toViewOrdering() }

    suspend fun setOrderings(orderings: List<Ordering>) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getOrderingDao().replaceBy(orderings.map { it.toDbOrdering() })
        }
    //endregion

    //region Queue

    fun <T : Any> getQueueItems(mapping: (DbQueueItemDisplay) -> T): Flow<PagingData<T>> =
        libraryDatabase.getQueueOrderDao().displayInQueueOrderPaging()
            .map(mapping)
            .convertToPagingFlow()

    fun getMediaIdsInQueueOrder() =
        libraryDatabase.getQueueOrderDao().mediaItemsInQueueOrderUpdatable()

    suspend fun getPositionForSong(songId: Long) =
        withContext(Dispatchers.IO) {
            libraryDatabase.getQueueOrderDao().findPositionInQueue(songId)
        }

    suspend fun getOrderlessSongQueue(
        filterParamList: List<Filter>,
        orderingList: List<Ordering>
    ): List<QueueItem.Song> =
        withContext(Dispatchers.IO) {
            if (filterParamList.flatten().none { it.type is TagFilterType }) {
                emptyList()
            } else {
                libraryDatabase.getSongDao().forCurrentFiltersList(
                    queryComposer.getQueryForSongIds(
                        filterParamList,
                        orderingList.toQueryOrderings()
                    )
                ).map {
                    QueueItem.Song(it)
                }
            }
        }

    suspend fun getOrderlessPodcastEpisodeQueue(
        filterParamList: List<Filter>
    ): List<QueueItem> =
        withContext(Dispatchers.IO) {
            if (filterParamList.flatten().none { it.type is PodcastFilterType }) {
                emptyList()
            } else {
                val rawQueryIdList = libraryDatabase
                    .getPodcastChapterDao()
                    .rawQueryIdList(
                        queryComposer.getQueryForPodcastEpisodesWithChapters(
                            filterParamList
                        )
                    )
                val handledEpisode = mutableListOf<Long>()
                val chaptersAndEpisodeWithoutChapters = rawQueryIdList
                    .flatMap {
                        val chapterId = it.chapterId
                        if (chapterId != null) {
                            if (handledEpisode.contains(it.podcastEpisodeId)) {
                                listOf(QueueItem.PodcastEpisodeChapter(chapterId))
                            } else {
                                handledEpisode.add(it.podcastEpisodeId)
                                listOf(
                                    QueueItem.PodcastEpisode(it.podcastEpisodeId),
                                    QueueItem.PodcastEpisodeChapter(chapterId)
                                )
                            }
                        } else {
                            listOf(QueueItem.PodcastEpisode(it.podcastEpisodeId))
                        }
                    }
                chaptersAndEpisodeWithoutChapters

            }
        }

    suspend fun saveQueueOrdering(listToSave: MutableList<QueueItem>) {
        libraryDatabase.getQueueOrderDao()
            .setOrder(listToSave.mapIndexed { index, queueItem ->
                DbQueueOrder(index, queueItem.id, queueItem.mediaType)
            })
    }
    //endregion

    sealed interface QueueItem {
        val mediaType: Int
        val id: Long

        data class Song(override val id: Long) : QueueItem {
            override val mediaType: Int = SONG_MEDIA_TYPE
        }

        data class PodcastEpisode(
            override val id: Long
        ) : QueueItem {
            override val mediaType: Int = PODCAST_MEDIA_TYPE
        }

        data class PodcastEpisodeChapter(override val id: Long) : QueueItem {
            override val mediaType: Int = PODCAST_CHAPTER_MEDIA_TYPE
        }
    }

    companion object {
        private const val HISTORY_SIZE = 100
    }
}