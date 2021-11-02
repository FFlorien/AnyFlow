package be.florien.anyflow.feature.alarms

import android.app.AlarmManager
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Alarm
import javax.inject.Inject

class AlarmsSynchronizer @Inject constructor(private val alarmManager: AlarmManager, private val dataRepository: DataRepository) {
    fun canScheduleExactAlarms() = true // Build.VERSION.SDK_INT >= 31 && alarmManager.canScheduleExactAlarms()

    fun getAlarms() = dataRepository.getAlarms()

    suspend fun addSingleAlarm(hour: Int, minute: Int) {
        val newAlarm = Alarm(0, hour, minute, false, listOf(false, false, false, false, false, false, false), active = true)
        addAlarmToDb(newAlarm)
    }

    suspend fun addRepeatingAlarm(hour: Int, minute: Int) {
        val newAlarm = Alarm(0, hour, minute, true, listOf(true, true, true, true, true, true, true), active = true)
        addAlarmToDb(newAlarm)

    }

    suspend fun addRepeatingAlarmForWeekDays(hour: Int, minute: Int, monday: Boolean, tuesday: Boolean, wednesday: Boolean, thursday: Boolean, friday: Boolean, saturday: Boolean, sunday: Boolean) {
        val newAlarm = Alarm(0, hour, minute, true, listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday), active = true)
        addAlarmToDb(newAlarm)

    }
    suspend fun toggleAlarm(alarm: Alarm) {
        if (!alarm.active) {
            dataRepository.activateAlarm(alarm)
        } else {
            dataRepository.deactivateAlarm(alarm)
        }
    }

    private suspend fun addAlarmToDb(alarm: Alarm) {
        dataRepository.addAlarm(alarm)
    }

    suspend fun updateAlarm(alarm: Alarm) {
        dataRepository.editAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        dataRepository.deleteAlarm(alarm)
    }
}