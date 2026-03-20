package be.florien.anyflow.tags.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.tags.local.model.DbFilterGroup
import be.florien.anyflow.tags.local.model.DbFilterGroup.Companion.CURRENT_FILTER_GROUP_ID
import be.florien.anyflow.tags.local.model.DbFilterGroupWithFilters
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FilterGroupDao : BaseDao<DbFilterGroup>() {
    // region SELECT
    @Query("SELECT * FROM filtergroup WHERE id = $CURRENT_FILTER_GROUP_ID")
    abstract suspend fun currentGroup(): DbFilterGroup

    @Query("SELECT * FROM filtergroup WHERE name = :name COLLATE NOCASE")
    abstract suspend fun filterGroupWithNameList(name: String): List<DbFilterGroup>

    @Query("SELECT * FROM filtergroup WHERE dateAdded NOT NULL AND name IS NULL")
    abstract suspend fun historyGroupsList(): List<DbFilterGroup>

    @Query("SELECT * FROM filtergroup WHERE id = $CURRENT_FILTER_GROUP_ID")
    abstract fun currentGroupUpdatable(): LiveData<DbFilterGroup>

    @Query("SELECT * FROM filtergroup WHERE dateAdded NOT NULL AND name IS NULL ORDER BY dateAdded DESC")
    abstract fun historyGroupsUpdatable(): Flow<List<DbFilterGroupWithFilters>>

    @Query("SELECT * FROM filtergroup WHERE name NOT NULL ORDER BY name COLLATE NOCASE")
    abstract fun savedGroupUpdatable(): Flow<List<DbFilterGroupWithFilters>>
    // endregion

    // region DELETE
    @Query("DELETE FROM filtergroup WHERE id = :id")
    abstract suspend fun deleteGroup(id: Long)

    @Query("DELETE FROM filtergroup")
    abstract suspend fun deleteAll()
    // endregion
}