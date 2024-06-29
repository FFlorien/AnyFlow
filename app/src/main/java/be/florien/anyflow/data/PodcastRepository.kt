package be.florien.anyflow.data

import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.local.model.DbPodcastEpisode
import be.florien.anyflow.extension.convertToPagingLiveData
import javax.inject.Inject

class PodcastRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase
) {
    fun <T : Any> getAllPodcastsEpisodes(convert: (DbPodcastEpisode) -> T) =
        libraryDatabase
            .getPodcastEpisodeDao()
            .getPodcastEpisodesPaging()
            .map(convert)
            .convertToPagingLiveData()

    suspend fun <T : Any> getAllPodcastsEpisodesList(convert: (DbPodcastEpisode) -> T) =
        libraryDatabase
            .getPodcastEpisodeDao()
            .getPodcastEpisodesList()
            .map(convert)

    suspend fun getPodcastDuration(id: Long) =
        libraryDatabase.getPodcastEpisodeDao().getPodcastDuration(id)

    fun getPodcastEpisode(id: Long) = libraryDatabase.getPodcastEpisodeDao().getPodcastEpisode(id)
}