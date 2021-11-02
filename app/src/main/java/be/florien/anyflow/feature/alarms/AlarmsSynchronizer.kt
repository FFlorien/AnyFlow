package be.florien.anyflow.feature.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Alarm
import be.florien.anyflow.extension.eLog
import java.util.*
import javax.inject.Inject

class AlarmsSynchronizer @Inject constructor(private val alarmManager: AlarmManager,
                                             private val dataRepository: DataRepository,
                                             private val alarmIntent: PendingIntent,
                                             private val playerIntent: PendingIntent) {
    fun canScheduleExactAlarms() = true // Build.VERSION.SDK_INT >= 31 && alarmManager.canScheduleExactAlarms()

    fun getAlarms() = dataRepository.getAlarms()

    suspend fun addSingleAlarm(hour: Int, minute: Int) {
        val newAlarm = Alarm(0, hour, minute, false, listOf(false, false, false, false, false, false, false), active = true)
        dataRepository.addAlarm(newAlarm)
        syncAlarms()
    }

    suspend fun addRepeatingAlarm(hour: Int, minute: Int) {
        val newAlarm = Alarm(0, hour, minute, true, listOf(true, true, true, true, true, true, true), active = true)
        dataRepository.addAlarm(newAlarm)
        syncAlarms()
    }

    suspend fun addRepeatingAlarmForWeekDays(hour: Int, minute: Int, monday: Boolean, tuesday: Boolean, wednesday: Boolean, thursday: Boolean, friday: Boolean, saturday: Boolean, sunday: Boolean) {
        val newAlarm = Alarm(0, hour, minute, true, listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday), active = true)
        dataRepository.addAlarm(newAlarm)
        syncAlarms()
    }

    suspend fun toggleAlarm(alarm: Alarm) {
        if (!alarm.active) {
            dataRepository.activateAlarm(alarm)
        } else {
            dataRepository.deactivateAlarm(alarm)
        }
        syncAlarms()
    }

    suspend fun updateAlarm(alarm: Alarm) {
        dataRepository.editAlarm(alarm)
        syncAlarms()
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        dataRepository.deleteAlarm(alarm)
        syncAlarms()
    }

    suspend fun syncAlarms() {
        alarmManager.cancel(playerIntent)
        val alarmList = dataRepository.getAlarmList()
        val nextOccurrence = alarmList.mapNotNull { it.getNextOccurrence() }.minByOrNull { it.timeInMillis }

        if (nextOccurrence != null) {
            setNextAlarm(nextOccurrence)
        }
    }

    private fun setNextAlarm(date: Calendar) {
        if (date.before(Calendar.getInstance())) {
            date.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(date.timeInMillis, alarmIntent), playerIntent)
    }
}