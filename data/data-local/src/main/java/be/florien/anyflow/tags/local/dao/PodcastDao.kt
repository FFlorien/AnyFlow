package be.florien.anyflow.tags.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.tags.local.model.DbPodcast
import be.florien.anyflow.tags.local.model.DbPodcastDisplay
import be.florien.anyflow.tags.local.model.DbPodcastFilterCount

@Dao
abstract class PodcastDao : BaseDao<DbPodcast>() {

    @Transaction
    open suspend fun updatePodcastList(podcasts: List<DbPodcast>) {
        val currentLocalPodcasts = getPodcastList()

        val deletedPodcasts = currentLocalPodcasts.filter { localPodcast ->
            podcasts.none { localPodcast.id == it.id }
        }
        delete(*deletedPodcasts.toTypedArray())

        val addedPodcasts = podcasts.filter { remotePodcast ->
            currentLocalPodcasts.none { remotePodcast.id == it.id }
        }
        upsert(addedPodcasts)

        val renamedPodcasts = podcasts.filter { remotePodcast ->
            val localPodcast = currentLocalPodcasts.firstOrNull { it.id == remotePodcast.id }
            localPodcast?.name != remotePodcast.name
        }
        upsert(renamedPodcasts)
    }

    @Query("SELECT id, name, syncDate FROM Podcast ORDER BY name DESC")
    abstract fun getPodcastPaging(): DataSource.Factory<Int, DbPodcastDisplay>

    @Query("SELECT * FROM podcast")
    abstract suspend fun getPodcastList(): List<DbPodcast>

    @Query("SELECT * FROM podcast")
    abstract fun getPodcastsUpdatable(): LiveData<List<DbPodcast>>

    @RawQuery
    abstract suspend fun getCount(query: SupportSQLiteQuery): DbPodcastFilterCount

    @RawQuery(observedEntities = [DbPodcast::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbPodcastDisplay>

}