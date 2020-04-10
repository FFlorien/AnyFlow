package be.florien.anyflow.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.data.local.model.DbQueueOrder

@Dao
abstract class QueueOrderDao : BaseDao<DbQueueOrder> {


    @Query("SELECT count(*) FROM queueorder")
    protected abstract suspend fun getCount(): Int

    @Query("DELETE FROM queueorder")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun setOrder(orderList: List<DbQueueOrder>) {
        deleteAll()
        insert(orderList)
    }
}