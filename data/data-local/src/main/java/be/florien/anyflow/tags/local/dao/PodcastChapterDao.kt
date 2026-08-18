package be.florien.anyflow.tags.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.tags.local.model.DbPodcastChapter
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeWithChapters

@Dao
abstract class PodcastChapterDao : BaseDao<DbPodcastChapter>() {

    @Transaction
    open suspend fun updatePodcastChapterList(podcastChapters: List<DbPodcastChapter>) {
        val currentLocalPodcastChapters = getPodcastChapterList()

        val deletedPodcastChapters = currentLocalPodcastChapters.filter { localChapter ->
            podcastChapters.none { localChapter.podcastEpisodeId == it.podcastEpisodeId }
        }
        delete(*deletedPodcastChapters.toTypedArray())
        upsert(podcastChapters)
    }

    @RawQuery(observedEntities = [DbPodcastChapter::class])
    abstract suspend fun rawQueryIdList(query: SupportSQLiteQuery): List<DbPodcastEpisodeWithChapters>

    @Query("SELECT * FROM PodcastChapter")
    abstract suspend fun getPodcastChapterList(): List<DbPodcastChapter>
}