package be.florien.ampacheplayer.persistence.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.model.Filter
import io.reactivex.Flowable


@Dao
interface FilterDao {
    @Query("SELECT * FROM filter")
    fun getFilter(): Flowable<List<Filter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(artists: List<Filter>)

    @Update
    fun update(vararg artists: Filter)

    @Delete
    fun delete(vararg artists: Filter)
}