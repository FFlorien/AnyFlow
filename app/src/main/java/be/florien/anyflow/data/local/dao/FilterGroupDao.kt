package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbFilterGroup
import be.florien.anyflow.data.local.model.DbFilterGroup.Companion.CURRENT_FILTER_GROUP_ID

@Dao
abstract class FilterGroupDao : BaseDao<DbFilterGroup>() {
    @Query("SELECT * FROM filtergroup WHERE id = $CURRENT_FILTER_GROUP_ID")
    abstract fun current(): LiveData<DbFilterGroup>

    @Query("SELECT * FROM filtergroup WHERE id = $CURRENT_FILTER_GROUP_ID")
    abstract suspend fun currentSync(): DbFilterGroup

    @Query("SELECT * FROM filtergroup WHERE dateAdded NOT NULL AND name = NULL")
    abstract fun history(): LiveData<List<DbFilterGroup>>

    @Query("SELECT * FROM filtergroup WHERE dateAdded NOT NULL AND name = NULL")
    abstract suspend fun historySync(): List<DbFilterGroup>

    @Query("SELECT * FROM filtergroup WHERE name NOT NULL")
    abstract fun saved(): LiveData<List<DbFilterGroup>>

    @Query("SELECT * FROM filtergroup WHERE name NOT NULL")
    abstract suspend fun savedSync(): List<DbFilterGroup>

    @Query("SELECT * FROM filtergroup WHERE name = :name COLLATE NOCASE")
    abstract suspend fun withNameIgnoreCase(name: String): List<DbFilterGroup>

    @Query("DELETE FROM filtergroup WHERE id = :id")
    abstract suspend fun deleteGroup(id: Long)

    @Query("DELETE FROM filtergroup")
    abstract suspend fun deleteAll()
}