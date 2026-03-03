package be.florien.anyflow.tags.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.tags.local.model.DbAlarm
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AlarmDao : BaseDao<DbAlarm>() {
    @Query("SELECT * FROM alarm")
    abstract suspend fun allList(): List<DbAlarm>

    @Query("SELECT * FROM alarm")
    abstract fun allAlarmsUpdatable(): Flow<List<DbAlarm>>

    @Query("SELECT * FROM alarm WHERE id = :id")
    abstract suspend fun getAlarm(id: Long): DbAlarm
}