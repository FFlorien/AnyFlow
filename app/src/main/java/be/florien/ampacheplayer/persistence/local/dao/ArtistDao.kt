package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.Artist
import io.reactivex.Flowable


@Dao
interface ArtistDao {
    @Query("SELECT * FROM artist ORDER BY name")
    fun getArtist(): Flowable<List<Artist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(artists: List<Artist>)

    @Update
    fun update(vararg artists: Artist)

    @Delete
    fun delete(vararg artists: Artist)
}