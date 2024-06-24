package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbPodcastEpisode

@Dao
abstract class PodcastEpisodeDao : BaseDao<DbPodcastEpisode>() {
    @Query("SELECT * FROM PodcastEpisode")
    abstract fun getPodcastEpisodes(): DataSource.Factory<Int, DbPodcastEpisode>

    @Query("SELECT * FROM PodcastEpisode WHERE podcastId = :podcastId")
    abstract fun getPodcastEpisodes(podcastId: String): LiveData<List<DbPodcastEpisode>>

    @RawQuery(observedEntities = [DbPodcastEpisode::class])
    abstract suspend fun forCurrentFilters(query: SupportSQLiteQuery): List<Long>

    @Query("SELECT * FROM PodcastEpisode")
    abstract suspend fun getPodcastEpisodesSync(): List<DbPodcastEpisode>

    @Query("DELETE FROM PodcastEpisode")
    abstract fun deleteAllPlaylistSongs()
}