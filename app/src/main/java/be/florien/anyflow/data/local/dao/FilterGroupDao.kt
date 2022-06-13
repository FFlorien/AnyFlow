package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbFilterGroup

@Dao
abstract class FilterGroupDao : BaseDao<DbFilterGroup>() {
    @Query("SELECT * FROM filtergroup")
    abstract fun all(): LiveData<List<DbFilterGroup>>

    @Query("SELECT * FROM filtergroup WHERE id != 1")
    abstract fun allSavedFilterGroup(): LiveData<List<DbFilterGroup>>

    @Query("SELECT * FROM filtergroup WHERE name = :name COLLATE NOCASE")
    abstract suspend fun withNameIgnoreCase(name: String): List<DbFilterGroup>

    @Query("DELETE FROM filtergroup WHERE id = :id")
    abstract suspend fun deleteGroup(id: Int)

    @Query("DELETE FROM filtergroup")
    abstract suspend fun deleteAll()
}