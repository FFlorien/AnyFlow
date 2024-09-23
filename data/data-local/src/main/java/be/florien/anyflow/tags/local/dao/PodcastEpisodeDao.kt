package be.florien.anyflow.tags.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.tags.local.model.DbMediaWaveForm

@Dao
abstract class PodcastEpisodeDao : BaseDao<DbPodcastEpisode>() {
    @Query("SELECT time FROM PodcastEpisode WHERE id = :id")
    abstract suspend fun getPodcastDuration(id: Long): Int

    @Query("SELECT waveForm FROM PodcastEpisode WHERE podcastepisode.id = :podcastEpisodeId")
    abstract suspend fun getWaveForm(podcastEpisodeId: Long): DbMediaWaveForm?

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

    @Query("UPDATE PodcastEpisode SET waveForm = :downSamples WHERE podcastepisode.id = :podcastEpisodeId")
    abstract suspend fun updateWithNewWaveForm(podcastEpisodeId: Long, downSamples: String?)

    @Query("DELETE FROM PodcastEpisode")
    abstract fun deleteAllPlaylistSongs()

    @Query("SELECT waveForm FROM PodcastEpisode WHERE podcastepisode.id = :podcastEpisodeId")
    abstract fun getWaveFormUpdatable(podcastEpisodeId: Long): LiveData<DbMediaWaveForm?>
}