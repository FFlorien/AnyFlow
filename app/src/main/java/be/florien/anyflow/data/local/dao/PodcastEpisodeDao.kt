package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbPodcast
import be.florien.anyflow.data.local.model.DbPodcastEpisode

@Dao
abstract class PodcastEpisodeDao : BaseDao<DbPodcastEpisode>() {
    @Query("SELECT * FROM PodcastEpisode")
    abstract fun getPodcastEpisodes(): LiveData<List<DbPodcastEpisode>>

    @Query("SELECT * FROM PodcastEpisode WHERE podcastId = :podcastId")
    abstract fun getPodcastEpisodes(podcastId: String): LiveData<List<DbPodcastEpisode>>

    @Query("SELECT * FROM podcast")
    abstract suspend fun getPodcastsSync(): List<DbPodcast>

    @Query("SELECT * FROM PodcastEpisode WHERE podcastId = :podcastId")
    abstract fun getPodcastEpisodesSync(podcastId: String): List<DbPodcastEpisode>
}