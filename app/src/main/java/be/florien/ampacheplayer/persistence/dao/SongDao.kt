package be.florien.ampacheplayer.persistence.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.model.Playlist
import be.florien.ampacheplayer.persistence.model.Song


@Dao
interface SongDao {
    @Query("SELECT * FROM song")
    fun getSong(): List<Song>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert( songs: List<Song>)

    @Update
    fun update(vararg songs: Song)

    @Delete
    fun delete(vararg songs: Song)
}