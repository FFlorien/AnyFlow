package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbAlarm

@Dao
interface AlarmDao : BaseDao<DbAlarm> {
    @Query("SELECT * FROM alarm")
    fun all(): LiveData<List<DbAlarm>>
    @Query("SELECT * FROM alarm")
    suspend fun list(): List<DbAlarm>
}