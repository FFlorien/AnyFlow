package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbAlarm

@Dao
abstract class AlarmDao : BaseDao<DbAlarm>() {
    @Query("SELECT * FROM alarm")
    abstract fun all(): LiveData<List<DbAlarm>>
    @Query("SELECT * FROM alarm")
    abstract suspend fun list(): List<DbAlarm>
}