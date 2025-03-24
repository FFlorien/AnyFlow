package be.florien.anyflow.management.podcast

import androidx.lifecycle.map
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterPodcastCount
import be.florien.anyflow.tags.local.LibraryDatabase
import be.florien.anyflow.tags.local.model.DbPodcast
import be.florien.anyflow.tags.local.model.DbPodcastDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeWithPodcast
import be.florien.anyflow.tags.local.query.QueryComposer
import javax.inject.Inject

class PodcastRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val queryComposer: QueryComposer
) {
    fun getPodcasts(
        filter: Filter?,
        search: String?
    ) = libraryDatabase
        .getPodcastDao()
        .rawQueryPaging(queryComposer.getQueryForPodcasts(filter))
        .map(DbPodcastDisplay::toViewPodcast)

    fun getPodcastsEpisodes(
        filter: Filter?,
        search: String?
    ) = libraryDatabase
            .getPodcastEpisodeDao()
            .rawQueryPaging(queryComposer.getQueryForPodcastEpisodes(filter))
            .map(DbPodcastEpisodeDisplay::toViewPodcastEpisode)

    suspend fun getAllPodcastsList() =
        libraryDatabase
            .getPodcastDao()
            .getPodcastList()
            .map(DbPodcast::toViewPodcast)

    suspend fun getAllPodcastsEpisodesList() =
        libraryDatabase
            .getPodcastEpisodeDao()
            .getPodcastEpisodesList()
            .map(DbPodcastEpisode::toViewPodcastEpisode)

    suspend fun getPodcastDuration(id: Long) =
        libraryDatabase.getPodcastEpisodeDao().getPodcastDuration(id)

    fun getPodcastEpisode(id: Long) = libraryDatabase.getPodcastEpisodeDao().getPodcastEpisode(id).map(DbPodcastEpisodeWithPodcast::toViewPodcastEpisode)

    fun getPodcastEpisodeDisplay(id: Long) = libraryDatabase.getPodcastEpisodeDao().getPodcastEpisodeDisplay(id)

    suspend fun getFilteredInfo(infoSource: Filter?): FilterPodcastCount {
        return libraryDatabase.getPodcastDao()
            .getCount(queryComposer.getQueryForPodcastCount(infoSource))
            .toViewFilterCount()
    }
}