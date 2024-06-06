package be.florien.anyflow.data

import androidx.lifecycle.map
import be.florien.anyflow.data.local.LibraryDatabase
import javax.inject.Inject

class PodcastRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase
) {

    fun getAllPodcastsEpisodes() = libraryDatabase.getPodcastEpisodeDao().getPodcastEpisodes()
        .map { list ->
            list.map { dbEpisode ->
                dbEpisode.toViewPodcastEpisode()
            }
        }


}