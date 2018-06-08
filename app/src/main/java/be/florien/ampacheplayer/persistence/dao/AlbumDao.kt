package be.florien.ampacheplayer.persistence.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.model.Album


@Dao
interface AlbumDao {
    @Query("SELECT * FROM album")
    fun getAlbum(): List<Album>

    @Insert
    fun insert( albums: List<Album>)

    @Update
    fun update(vararg albums: Album)

    @Delete
    fun delete(vararg albums: Album)
}