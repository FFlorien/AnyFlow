package be.florien.ampacheplayer.persistence.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.model.QueueOrder


@Dao
interface QueueOrderDao {
    @Query("SELECT * FROM queueorder")
    fun getQueueOrder(): List<QueueOrder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert( queueOrders: List<QueueOrder>)

    @Update
    fun update(vararg queueOrders: QueueOrder)

    @Delete
    fun delete(vararg queueOrders: QueueOrder)
}