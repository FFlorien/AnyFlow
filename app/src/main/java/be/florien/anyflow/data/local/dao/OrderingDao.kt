package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.data.local.model.DbOrdering

@Dao
abstract class OrderingDao : BaseDao<DbOrdering>() {
    @Query("SELECT * FROM ordering ORDER BY priority")
    abstract fun all(): LiveData<List<DbOrdering>>

    @Query("SELECT * FROM ordering ORDER BY priority")
    abstract fun list(): List<DbOrdering>

    @Query("DELETE FROM ordering")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceBy(filters: List<DbOrdering>) {
        deleteAll()
        insert(filters)
    }
}