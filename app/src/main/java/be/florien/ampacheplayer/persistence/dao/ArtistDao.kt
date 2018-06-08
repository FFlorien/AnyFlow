package be.florien.ampacheplayer.persistence.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.model.Artist


@Dao
interface ArtistDao {
    @Query("SELECT * FROM artist")
    fun getArtist(): List<Artist>

    @Insert
    fun insert( artists: List<Artist>)

    @Update
    fun update(vararg artists: Artist)

    @Delete
    fun delete(vararg artists: Artist)
}