package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbPodcast

@Dao
abstract class PodcastDao : BaseDao<DbPodcast>() {
    @Query("SELECT * FROM podcast")
    abstract fun getPodcasts(): LiveData<List<DbPodcast>>

    @Query("SELECT * FROM podcast")
    abstract suspend fun getPodcastsSync(): List<DbPodcast>

    @Query("SELECT * FROM podcast JOIN podcastepisode on podcastepisode.podcastId = podcast.id")
    abstract fun getPodcastsWithEpisodes(): LiveData<List<DbPodcast>>

    @Query("SELECT * FROM podcast JOIN podcastepisode on podcastepisode.podcastId = podcast.id")
    abstract suspend fun getPodcastsWithEpisodesSync(): List<DbPodcast>
}