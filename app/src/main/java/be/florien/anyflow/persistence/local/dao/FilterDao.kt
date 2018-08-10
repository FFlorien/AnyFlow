package be.florien.anyflow.persistence.local.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import be.florien.anyflow.persistence.local.model.DbFilter
import io.reactivex.Flowable

@Dao
abstract class FilterDao : BaseDao<DbFilter> {
    @Query("SELECT * FROM dbfilter")
    abstract fun all(): Flowable<List<DbFilter>>

    @Query("DELETE FROM dbfilter")
    abstract fun deleteAll()

    @Transaction
    open fun replaceBy(filters: List<DbFilter>) {
        deleteAll()
        insert(filters)
    }
}