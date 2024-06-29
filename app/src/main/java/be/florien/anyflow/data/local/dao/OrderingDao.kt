package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.data.local.model.DbOrdering

@Dao
abstract class OrderingDao : BaseDao<DbOrdering>() {
    // region SELECT
    @Query("SELECT * FROM ordering ORDER BY priority")
    abstract fun allUpdatable(): LiveData<List<DbOrdering>>

    @Query("SELECT * FROM ordering ORDER BY priority")
    abstract fun allList(): List<DbOrdering>
    //endregion

    //region INSERT
    @Transaction
    open suspend fun replaceBy(filters: List<DbOrdering>) {
        deleteAll()
        insertList(filters)
    }
    //endregion

    @Query("DELETE FROM ordering")
    abstract suspend fun deleteAll()
}