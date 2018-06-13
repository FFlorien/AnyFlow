package be.florien.ampacheplayer.persistence.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.model.QueueOrder
import be.florien.ampacheplayer.persistence.model.Song
import io.reactivex.Flowable


@Dao
interface QueueOrderDao {
    @Query("SELECT * FROM queueorder")
    fun getQueueOrder(): Flowable<List<QueueOrder>>

    @Query("SELECT * FROM song JOIN queueorder ON song.id is queueorder.songId ORDER BY queueorder.`order`")
    fun getSongsInQueueOrder(): Flowable<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert( queueOrders: List<QueueOrder>)

    @Update
    fun update(vararg queueOrders: QueueOrder)

    @Delete
    fun delete(vararg queueOrders: QueueOrder)
}