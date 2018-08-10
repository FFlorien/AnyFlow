package be.florien.anyflow.persistence.local.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import be.florien.anyflow.persistence.local.model.DbOrder
import io.reactivex.Flowable

@Dao
abstract class OrderDao : BaseDao<DbOrder> {
    @Query("SELECT * FROM dborder ORDER BY priority")
    abstract fun all(): Flowable<List<DbOrder>>

    @Query("DELETE FROM dborder")
    abstract fun deleteAll()

    @Transaction
    open fun replaceBy(filters: List<DbOrder>) {
        deleteAll()
        insert(filters)
    }
}