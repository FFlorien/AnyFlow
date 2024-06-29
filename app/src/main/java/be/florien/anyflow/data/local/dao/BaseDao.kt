package be.florien.anyflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery


@Dao
abstract class BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertList(items: List<T>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertItem(item: T): Long

    @Update
    abstract suspend fun updateItems(vararg items: T)

    @Update
    abstract suspend fun updateList(items: List<T>)

    @Delete
    abstract suspend fun delete(vararg items: T)

    @RawQuery
    abstract suspend fun rawQueryList(query: SupportSQLiteQuery): List<T>

    // Do not abstract rawQuery for observable (LiveData, Paging, Flow)
    // because it will not be updated due to the lack of observedEntities in the annotation

    @Transaction
    open suspend fun upsert(obj: T) {
        val id: Long = insertItem(obj)
        if (id == -1L) {
            updateItems(obj)
        }
    }

    @Transaction
    open suspend fun upsert(objList: List<T>) {
        val insertResult: List<Long> = insertList(objList)
        val updateList: MutableList<T> = mutableListOf()
        for (i in insertResult.indices) {
            if (insertResult[i] == -1L) {
                updateList.add(objList[i])
            }
        }
        if (updateList.isNotEmpty()) {
            updateList(updateList)
        }
    }
}