package be.florien.anyflow.tags.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.tags.local.model.DbPodcast

@Dao
abstract class PodcastDao : BaseDao<DbPodcast>() {
    @Query("SELECT * FROM podcast")
    abstract suspend fun getPodcastList(): List<DbPodcast>

    @Query("SELECT * FROM podcast")
    abstract fun getPodcastsUpdatable(): LiveData<List<DbPodcast>>

}