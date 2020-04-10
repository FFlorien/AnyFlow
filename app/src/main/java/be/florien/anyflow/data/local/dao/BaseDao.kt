package be.florien.anyflow.data.local.dao

import androidx.room.*

@Dao
interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<T>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingle(item: T): Long

    @Update
    suspend fun update(vararg items: T)

    @Delete
    suspend fun delete(vararg items: T)
}