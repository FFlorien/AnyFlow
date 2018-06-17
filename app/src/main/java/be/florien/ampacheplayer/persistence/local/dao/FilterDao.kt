package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.Filter
import io.reactivex.Flowable


@Dao
interface FilterDao {
    @Query("SELECT * FROM dbfilter")
    fun getFilters(): Flowable<List<Filter.DbFilter>>

    @Query("SELECT * FROM dbfilter")
    fun getFiltersSync(): List<Filter.DbFilter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(artists: List<Filter.DbFilter>)

    @Update
    fun update(vararg artists: Filter.DbFilter)

    @Delete
    fun delete(vararg artists: Filter.DbFilter)

    @Query("DELETE FROM dbfilter")
    fun deleteAll()
}