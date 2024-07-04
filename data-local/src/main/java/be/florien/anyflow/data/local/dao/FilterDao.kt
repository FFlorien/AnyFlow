package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbFilter
import be.florien.anyflow.data.local.model.DbSongFilterCount
import be.florien.anyflow.data.local.model.DbFilterGroup

@Dao
abstract class FilterDao : BaseDao<DbFilter>() {
    // region SELECT
    @RawQuery
    abstract suspend fun getCount(query: SupportSQLiteQuery): DbSongFilterCount

    @Query("SELECT filter.id, type, argument, displayText, filterGroup, parentFilter FROM filter JOIN filtergroup ON filter.filterGroup = filterGroup.id WHERE filterGroup.id = ${DbFilterGroup.CURRENT_FILTER_GROUP_ID}")
    abstract fun currentFilterList(): List<DbFilter>

    @Query("SELECT * FROM filter WHERE filterGroup = :groupId")
    abstract suspend fun filtersForGroupList(groupId: Long): List<DbFilter>

    @Query("SELECT filter.id, type, argument, displayText, filterGroup, parentFilter FROM filter JOIN filtergroup ON filter.filterGroup = filterGroup.id WHERE filterGroup.id = ${DbFilterGroup.CURRENT_FILTER_GROUP_ID}")
    abstract fun currentFiltersUpdatable(): LiveData<List<DbFilter>>
    // endregion

    //region INSERT
    @Transaction
    open suspend fun updateGroup(group: DbFilterGroup, filters: List<DbFilter>) {
        deleteGroupSync(group.id)
        insertList(filters)
    }
    //endregion

    //region DELETE
    @Query("DELETE FROM filter WHERE filterGroup = :groupId")
    abstract fun deleteGroupSync(groupId: Long)
    //endregion
}