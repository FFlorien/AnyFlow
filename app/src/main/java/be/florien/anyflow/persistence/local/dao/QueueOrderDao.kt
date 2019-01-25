package be.florien.anyflow.persistence.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.persistence.local.model.QueueOrder

@Dao
abstract class QueueOrderDao : BaseDao<QueueOrder> {


    @Query("SELECT count(*) FROM queueorder")
    protected abstract fun getCount(): Int

    @Query("DELETE FROM queueorder")
    abstract fun deleteAll()

    fun hasQueue() = getCount() > 0

    @Transaction
    open fun setOrder(orderList: List<QueueOrder>) {
        deleteAll()
        insert(orderList)
    }
}