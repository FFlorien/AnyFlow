package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbPlaylist

@Dao
interface PlaylistDao : BaseDao<DbPlaylist> {
    @Query("SELECT * FROM playlist")
    fun getPlaylist(): LiveData<List<DbPlaylist>>
}