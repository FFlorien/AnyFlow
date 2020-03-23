package be.florien.anyflow.data.local.dao

import androidx.room.*

@Dao
interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(items: List<T>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSingle(item: T): Long

    @Update
    fun update(vararg items: T)

    @Delete
    fun delete(vararg items: T)
}