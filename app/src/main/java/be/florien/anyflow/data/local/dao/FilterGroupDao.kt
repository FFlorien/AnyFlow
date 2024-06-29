package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbFilterGroup
import be.florien.anyflow.data.local.model.DbFilterGroup.Companion.CURRENT_FILTER_GROUP_ID

@Dao
abstract class FilterGroupDao : BaseDao<DbFilterGroup>() {
    // region SELECT
    @Query("SELECT * FROM filtergroup WHERE id = $CURRENT_FILTER_GROUP_ID")
    abstract suspend fun currentGroup(): DbFilterGroup

    @Query("SELECT * FROM filtergroup WHERE name = :name COLLATE NOCASE")
    abstract suspend fun filterGroupWithNameList(name: String): List<DbFilterGroup>

    @Query("SELECT * FROM filtergroup WHERE dateAdded NOT NULL AND name = NULL")
    abstract suspend fun historyGroupsList(): List<DbFilterGroup>

    @Query("SELECT * FROM filtergroup WHERE id = $CURRENT_FILTER_GROUP_ID")
    abstract fun currentGroupUpdatable(): LiveData<DbFilterGroup>

    @Query("SELECT * FROM filtergroup WHERE dateAdded NOT NULL AND name = NULL")
    abstract fun historyGroupsUpdatable(): LiveData<List<DbFilterGroup>>

    @Query("SELECT * FROM filtergroup WHERE name NOT NULL")
    abstract fun savedGroupUpdatable(): LiveData<List<DbFilterGroup>>
    // endregion

    // region DELETE
    @Query("DELETE FROM filtergroup WHERE id = :id")
    abstract suspend fun deleteGroup(id: Long)

    @Query("DELETE FROM filtergroup")
    abstract suspend fun deleteAll()
    // endregion
}