package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.Album
import io.reactivex.Flowable


@Dao
interface AlbumDao {
    @Query("SELECT * FROM album")
    fun getAlbum(): Flowable<List<Album>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(albums: List<Album>)

    @Update
    fun update(vararg albums: Album)

    @Delete
    fun delete(vararg albums: Album)
}