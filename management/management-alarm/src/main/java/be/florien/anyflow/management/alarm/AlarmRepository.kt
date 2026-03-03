package be.florien.anyflow.management.alarm

import be.florien.anyflow.management.alarm.model.Alarm
import be.florien.anyflow.tags.local.LibraryDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AlarmRepository @Inject constructor(private val libraryDatabase: LibraryDatabase) {

    suspend fun addAlarm(alarm: Alarm) =
        libraryDatabase.getAlarmDao().insertItem(alarm.toDbAlarm())

    suspend fun getAlarm(id: Long): Alarm =
        libraryDatabase.getAlarmDao().getAlarm(id).toViewAlarm()

    fun getAlarms(): Flow<List<Alarm>> =
        libraryDatabase.getAlarmDao().allAlarmsUpdatable().map { list -> list.map { it.toViewAlarm() } }

    suspend fun getAlarmList(): List<Alarm> =
        libraryDatabase.getAlarmDao().allList().map { it.toViewAlarm() }

    suspend fun toggleAlarm(id: Long) {
        val alarm = libraryDatabase.getAlarmDao().getAlarm(id)
        val newAlarm = alarm.copy(active = !alarm.active)
        libraryDatabase.getAlarmDao().updateItems(newAlarm)
    }

    suspend fun editAlarm(alarm: Alarm) {
        libraryDatabase.getAlarmDao().updateItems(alarm.toDbAlarm())
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        libraryDatabase.getAlarmDao().delete(alarm.toDbAlarm())
    }
}