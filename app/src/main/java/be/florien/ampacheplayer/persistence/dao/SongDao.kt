package be.florien.ampacheplayer.persistence.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.model.Song
import io.reactivex.Flowable


@Dao
interface SongDao {
    @Query("SELECT * FROM song ORDER BY song.albumArtistName, song.albumName, song.track")
    fun getSong(): Flowable<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(songs: List<Song>)

    @Update
    fun update(vararg songs: Song)

    @Delete
    fun delete(vararg songs: Song)
}