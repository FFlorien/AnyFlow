package be.florien.anyflow.management.podcast

import be.florien.anyflow.tags.local.LibraryDatabase
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import javax.inject.Inject

class PodcastRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase
) {
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
}