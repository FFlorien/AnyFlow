package be.florien.anyflow.persistence.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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