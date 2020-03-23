package be.florien.anyflow.data.local.dao

import androidx.room.*
import be.florien.anyflow.data.local.model.Playlist
import io.reactivex.Flowable

@Dao
interface PlaylistDao : BaseDao<Playlist> {
    @Query("SELECT * FROM playlist")
    fun getPlaylist(): Flowable<List<Playlist>>
}