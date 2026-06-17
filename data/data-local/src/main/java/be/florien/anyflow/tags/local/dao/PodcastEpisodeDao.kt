package be.florien.anyflow.tags.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.tags.local.model.DbMediaWaveForm
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeWithPodcast

@Dao
abstract class PodcastEpisodeDao : BaseDao<DbPodcastEpisode>() {

    @Transaction
    open suspend fun updatePodcastEpisodeList(podcastEpisodes: List<DbPodcastEpisode>) {
        val currentLocalPodcastEpisodes = getPodcastEpisodesList()

        val deletedPodcastEpisodes = currentLocalPodcastEpisodes.filter { localPodcast ->
            podcastEpisodes.none { localPodcast.id == it.id }
        }
        delete(*deletedPodcastEpisodes.toTypedArray())
        upsert(podcastEpisodes)
    }

    @Query("SELECT time FROM PodcastEpisode WHERE id = :id")
    abstract suspend fun getPodcastDuration(id: Long): Int

    @Query("SELECT waveForm FROM PodcastEpisode WHERE podcastepisode.id = :podcastEpisodeId")
    abstract suspend fun getWaveForm(podcastEpisodeId: Long): DbMediaWaveForm?

    @RawQuery(observedEntities = [DbPodcastEpisode::class])
    abstract suspend fun rawQueryIdList(query: SupportSQLiteQuery): List<Long>

    @RawQuery(observedEntities = [DbPodcastEpisode::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbPodcastEpisodeDisplay>

    @Query("SELECT * FROM PodcastEpisode ORDER BY publicationDate DESC")
    abstract suspend fun getPodcastEpisodesList(): List<DbPodcastEpisode>

    @Query("SELECT PodcastEpisode.id, PodcastEpisode.title, Podcast.name as podcastName, Podcast.id as podcastId, PodcastEpisode.time, PodcastEpisode.description, PodcastEpisode.publicationDate FROM PodcastEpisode JOIN Podcast ON PodcastEpisode.podcastId = Podcast.id ORDER BY publicationDate DESC")
    abstract fun getPodcastEpisodesPaging(): DataSource.Factory<Int, DbPodcastEpisodeDisplay>

    @Query("SELECT * FROM PodcastEpisode WHERE podcastId = :podcastId")
    abstract fun getPodcastEpisodesUpdatable(podcastId: String): LiveData<List<DbPodcastEpisode>>

    @Transaction
    @Query("SELECT * FROM PodcastEpisode WHERE PodcastEpisode.id = :id")
    abstract fun getPodcastEpisode(id: Long): LiveData<DbPodcastEpisodeWithPodcast>

    @Query("SELECT PodcastEpisode.id, PodcastEpisode.title, Podcast.name as podcastName, Podcast.id as podcastId, PodcastEpisode.time, PodcastEpisode.description, PodcastEpisode.publicationDate FROM PodcastEpisode JOIN Podcast ON PodcastEpisode.podcastId = Podcast.id WHERE PodcastEpisode.id = :id")
    abstract fun getPodcastEpisodeDisplay(id: Long): DbPodcastEpisodeDisplay?

    @Query("UPDATE PodcastEpisode SET waveForm = :downSamples WHERE podcastepisode.id = :podcastEpisodeId")
    abstract suspend fun updateWithNewWaveForm(podcastEpisodeId: Long, downSamples: String?)

    @Query("DELETE FROM PodcastEpisode")
    abstract fun deleteAllPodcastEpisodes()

    @Query("SELECT waveForm FROM PodcastEpisode WHERE podcastepisode.id = :podcastEpisodeId")
    abstract fun getWaveFormUpdatable(podcastEpisodeId: Long): LiveData<DbMediaWaveForm?>
}