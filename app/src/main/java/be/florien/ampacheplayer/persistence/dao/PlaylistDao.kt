package be.florien.ampacheplayer.persistence.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.model.Playlist
import io.reactivex.Flowable


@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist")
    fun getPlaylist(): Flowable<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(playlists: List<Playlist>)

    @Update
    fun update(vararg playlists: Playlist)

    @Delete
    fun delete(vararg playlists: Playlist)
}