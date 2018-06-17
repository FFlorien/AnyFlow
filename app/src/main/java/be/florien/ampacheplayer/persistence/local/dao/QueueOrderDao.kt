package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.QueueOrder
import be.florien.ampacheplayer.persistence.local.model.Song
import io.reactivex.Flowable


@Dao
interface QueueOrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert( queueOrders: List<QueueOrder>)

    @Update
    fun update(vararg queueOrders: QueueOrder)

    @Delete
    fun delete(vararg queueOrders: QueueOrder)

    @Query("DELETE FROM queueorder")
    fun deleteAll()
}