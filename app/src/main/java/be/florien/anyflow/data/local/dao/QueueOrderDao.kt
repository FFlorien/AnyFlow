package be.florien.anyflow.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.data.local.model.QueueOrder

@Dao
abstract class QueueOrderDao : BaseDao<QueueOrder> {


    @Query("SELECT count(*) FROM queueorder")
    protected abstract fun getCount(): Int

    @Query("DELETE FROM queueorder")
    abstract fun deleteAll()

    @Transaction
    open fun setOrder(orderList: List<QueueOrder>) {
        deleteAll()
        insert(orderList)
    }
}