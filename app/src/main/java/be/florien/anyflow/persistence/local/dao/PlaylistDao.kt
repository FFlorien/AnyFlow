package be.florien.anyflow.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.anyflow.persistence.local.model.Playlist
import io.reactivex.Flowable

@Dao
interface PlaylistDao : BaseDao<Playlist> {
    @Query("SELECT * FROM playlist")
    fun getPlaylist(): Flowable<List<Playlist>>
}