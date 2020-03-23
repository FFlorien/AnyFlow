package be.florien.anyflow.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.data.local.model.DbFilter
import be.florien.anyflow.data.local.model.FilterGroup
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class FilterDao : BaseDao<DbFilter> {
    @Query("SELECT * FROM dbfilter WHERE filterGroup = 1")
    abstract fun currentFilters(): Flowable<List<DbFilter>>

    @Query("SELECT * FROM dbfilter WHERE filterGroup = :groupId")
    abstract fun filterForGroupSync(groupId: Long): List<DbFilter>

    @Query("DELETE FROM dbfilter WHERE filterGroup = :groupId")
    abstract fun deleteGroupSync(groupId: Long)

    @Query("SELECT * FROM dbfilter WHERE filterGroup = :groupId")
    abstract fun filterForGroupAsync(groupId: Long): Single<List<DbFilter>>

    @Transaction
    open fun updateGroup(group: FilterGroup, filters: List<DbFilter>) {
        deleteGroupSync(group.id)
        insert(filters)
    }
}