package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbFilter
import be.florien.anyflow.data.local.model.DbFilterCount
import be.florien.anyflow.data.local.model.DbFilterGroup

@Dao
abstract class FilterDao : BaseDao<DbFilter>() {
    @Query("SELECT * FROM filter JOIN filtergroup ON filter.filterGroup = filterGroup.id WHERE filterGroup.dateAdded = null AND filterGroup.name = null")
    abstract fun currentFilters(): LiveData<List<DbFilter>>

    @Query("SELECT * FROM filter WHERE filterGroup = :groupId")
    abstract suspend fun filterForGroup(groupId: Long): List<DbFilter>

    @Query("DELETE FROM filter WHERE filterGroup = :groupId")
    abstract suspend fun deleteGroupSync(groupId: Long)

    @Transaction
    open suspend fun updateGroup(group: DbFilterGroup, filters: List<DbFilter>) {
        deleteGroupSync(group.id)
        insert(filters)
    }

    @RawQuery
    abstract suspend fun getCount(query: SupportSQLiteQuery): DbFilterCount
}