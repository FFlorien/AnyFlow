package be.florien.anyflow.data.local.dao

import androidx.room.*
import be.florien.anyflow.data.local.model.DbPlaylist
import io.reactivex.Flowable

@Dao
interface PlaylistDao : BaseDao<DbPlaylist> {
    @Query("SELECT * FROM playlist")
    fun getPlaylist(): Flowable<List<DbPlaylist>>
}