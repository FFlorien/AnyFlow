package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.DbFilter
import be.florien.ampacheplayer.persistence.local.model.Filter
import io.reactivex.Flowable


@Dao
interface FilterDao {
    @Query("SELECT * FROM dbfilter")
    fun getFilters(): Flowable<List<DbFilter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(artists: List<DbFilter>)

    @Update
    fun update(vararg artists: DbFilter)

    @Delete
    fun delete(vararg artists: DbFilter)

    @Query("DELETE FROM dbfilter")
    fun deleteAll()
}