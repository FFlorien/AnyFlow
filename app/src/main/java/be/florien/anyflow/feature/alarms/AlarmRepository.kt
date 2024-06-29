package be.florien.anyflow.feature.alarms

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.toDbAlarm
import be.florien.anyflow.data.toViewAlarm
import be.florien.anyflow.data.view.Alarm
import javax.inject.Inject

class AlarmRepository @Inject constructor(private val libraryDatabase: LibraryDatabase) {

    suspend fun addAlarm(alarm: Alarm) =
        libraryDatabase.getAlarmDao().insertItem(alarm.toDbAlarm())

    fun getAlarms(): LiveData<List<Alarm>> =
        libraryDatabase.getAlarmDao().allAlarmsUpdatable().map { list -> list.map { it.toViewAlarm() } }

    suspend fun getAlarmList(): List<Alarm> =
        libraryDatabase.getAlarmDao().allList().map { it.toViewAlarm() }

    suspend fun activateAlarm(alarm: Alarm) {
        val newAlarm = alarm.copy(active = true)
        libraryDatabase.getAlarmDao().updateItems(newAlarm.toDbAlarm())
    }

    suspend fun deactivateAlarm(alarm: Alarm) {
        val newAlarm = alarm.copy(active = false)
        libraryDatabase.getAlarmDao().updateItems(newAlarm.toDbAlarm())
    }

    suspend fun editAlarm(alarm: Alarm) {
        libraryDatabase.getAlarmDao().updateItems(alarm.toDbAlarm())
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        libraryDatabase.getAlarmDao().delete(alarm.toDbAlarm())
    }
}