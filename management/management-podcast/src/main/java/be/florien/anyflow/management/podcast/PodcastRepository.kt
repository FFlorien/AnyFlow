package be.florien.anyflow.management.podcast

import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.FilterPodcastCount
import be.florien.anyflow.tags.local.LibraryDatabase
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.tags.local.query.QueryComposer
import be.florien.anyflow.tags.toQueryFilters
import javax.inject.Inject

class PodcastRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase
) {
    private val queryComposer: QueryComposer = QueryComposer()

    fun getAllPodcastsEpisodes() =
        libraryDatabase
            .getPodcastEpisodeDao()
            .getPodcastEpisodesPaging()
            .map(DbPodcastEpisode::toViewPodcastEpisode)

    suspend fun getAllPodcastsEpisodesList() =
        libraryDatabase
            .getPodcastEpisodeDao()
            .getPodcastEpisodesList()
            .map(DbPodcastEpisode::toViewPodcastEpisode)

    suspend fun getPodcastDuration(id: Long) =
        libraryDatabase.getPodcastEpisodeDao().getPodcastDuration(id)

    fun getPodcastEpisode(id: Long) = libraryDatabase.getPodcastEpisodeDao().getPodcastEpisode(id)

    suspend fun getFilteredInfo(infoSource: Filter<*>?): FilterPodcastCount {
        val filterList = infoSource?.let { listOf(it) } ?: emptyList()
        return libraryDatabase.getPodcastDao()
            .getCount(queryComposer.getQueryForPodcastCount(filterList.toQueryFilters()))
            .toViewFilterCount()
    }
}