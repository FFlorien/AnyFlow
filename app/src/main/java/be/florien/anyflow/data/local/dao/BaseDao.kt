package be.florien.anyflow.data.local.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery


@Dao
abstract class BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(items: List<T>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSingle(item: T): Long

    @Update
    abstract suspend fun update(vararg items: T)

    @Update
    abstract suspend fun updateAll(items: List<T>)

    @Delete
    abstract suspend fun delete(vararg items: T)

    @RawQuery
    abstract suspend fun rawQuery(query: SupportSQLiteQuery): List<T>

    @Transaction
    open suspend fun upsert(obj: T) {
        val id: Long = insertSingle(obj)
        if (id == -1L) {
            update(obj)
        }
    }

    @Transaction
    open suspend fun upsert(objList: List<T>) {
        val insertResult: List<Long> = insert(objList)
        val updateList: MutableList<T> = mutableListOf()
        for (i in insertResult.indices) {
            if (insertResult[i] == -1L) {
                updateList.add(objList[i])
            }
        }
        if (updateList.isNotEmpty()) {
            updateAll(updateList)
        }
    }
}