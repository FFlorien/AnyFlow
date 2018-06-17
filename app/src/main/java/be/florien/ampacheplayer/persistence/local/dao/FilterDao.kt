package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.Filter
import io.reactivex.Flowable

//
//@Dao
//interface FilterDao {
//    @Query("SELECT * FROM filter")
//    fun getFilters(): List<Filter>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insert(artists: List<Filter>)
//
//    @Update
//    fun update(vararg artists: Filter)
//
//    @Delete
//    fun delete(vararg artists: Filter)
//
//    @Query("DELETE FROM filter")
//    fun deleteAll()
//}