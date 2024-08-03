package be.florien.anyflow.tags.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.tags.local.model.DbPodcastEpisode

@Dao
abstract class PodcastEpisodeDao : BaseDao<DbPodcastEpisode>() {
    @Query("SELECT time FROM PodcastEpisode WHERE id = :id")
    abstract suspend fun getPodcastDuration(id: Long): Int

    @RawQuery(observedEntities = [DbPodcastEpisode::class])
    abstract suspend fun rawQueryIdList(query: SupportSQLiteQuery): List<Long>

    @Query("SELECT * FROM PodcastEpisode ORDER BY publicationDate DESC")
    abstract suspend fun getPodcastEpisodesList(): List<DbPodcastEpisode>

    @Query("SELECT * FROM PodcastEpisode ORDER BY publicationDate DESC")
    abstract fun getPodcastEpisodesPaging(): DataSource.Factory<Int, DbPodcastEpisode>

    @Query("SELECT * FROM PodcastEpisode WHERE podcastId = :podcastId")
    abstract fun getPodcastEpisodesUpdatable(podcastId: String): LiveData<List<DbPodcastEpisode>>

    @Query("SELECT * FROM PodcastEpisode WHERE id = :id")
    abstract fun getPodcastEpisode(id: Long): LiveData<DbPodcastEpisode>

    @Query("DELETE FROM PodcastEpisode")
    abstract fun deleteAllPlaylistSongs()
}