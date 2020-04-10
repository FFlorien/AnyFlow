package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.data.local.model.DbOrder

@Dao
abstract class OrderDao : BaseDao<DbOrder> {
    @Query("SELECT * FROM dborder ORDER BY priority")
    abstract fun all(): LiveData<List<DbOrder>>

    @Query("DELETE FROM dborder")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceBy(filters: List<DbOrder>) {
        deleteAll()
        insert(filters)
    }
}