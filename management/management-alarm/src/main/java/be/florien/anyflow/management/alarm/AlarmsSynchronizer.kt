package be.florien.anyflow.management.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import be.florien.anyflow.management.alarm.model.Alarm
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class AlarmsSynchronizer @Inject constructor(
    private val alarmManager: AlarmManager,
    private val alarmRepository: AlarmRepository,
    @Named("player") private val playerIntent: PendingIntent,
    @Named("alarm") private val alarmIntent: PendingIntent
) {
    fun canScheduleExactAlarms() =
        true // Build.VERSION.SDK_INT >= 31 && alarmManager.canScheduleExactAlarms()

    fun getAlarms() = alarmRepository.getAlarms()

    suspend fun addSingleAlarm(hour: Int, minute: Int) {
        val recurrence = false
        val newAlarm = Alarm(
            0,
            hour,
            minute,
            false,
            getWeekRecurrence(recurrence),
            active = true
        )
        alarmRepository.addAlarm(newAlarm)
        syncAlarms()
    }

    suspend fun addRepeatingAlarm(hour: Int, minute: Int) {
        val newAlarm = Alarm(
            0,
            hour,
            minute,
            true,
            getWeekRecurrence(true),
            active = true
        )
        alarmRepository.addAlarm(newAlarm)
        syncAlarms()
    }

    suspend fun addRepeatingAlarmForWeekDays(
        hour: Int,
        minute: Int,
        monday: Boolean,
        tuesday: Boolean,
        wednesday: Boolean,
        thursday: Boolean,
        friday: Boolean,
        saturday: Boolean,
        sunday: Boolean
    ) {
        val newAlarm = Alarm(
            0,
            hour,
            minute,
            true,
            listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday),
            active = true
        )
        alarmRepository.addAlarm(newAlarm)
        syncAlarms()
    }

    suspend fun toggleAlarm(alarm: Alarm) {
        if (!alarm.active) {
            alarmRepository.activateAlarm(alarm)
        } else {
            alarmRepository.deactivateAlarm(alarm)
        }
        syncAlarms()
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmRepository.editAlarm(alarm)
        syncAlarms()
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmRepository.deleteAlarm(alarm)
        syncAlarms()
    }

    suspend fun syncAlarms() {
        alarmManager.cancel(alarmIntent)
        val alarmList = alarmRepository.getAlarmList()
        val nextOccurrence = alarmList
            .mapNotNull { it.getNextOccurrence() }
            .minByOrNull { it.timeInMillis }

        if (nextOccurrence != null) {
            setNextAlarm(nextOccurrence)
        }
    }

    private fun getWeekRecurrence(recurrence: Boolean) = listOf(
        recurrence,
        recurrence,
        recurrence,
        recurrence,
        recurrence,
        recurrence,
        recurrence
    )

    private fun setNextAlarm(date: Calendar) {
        if (date.before(Calendar.getInstance())) {
            date.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(date.timeInMillis, alarmIntent),
                    playerIntent
                )
            }
        } else {
            //TODO("VERSION.SDK_INT < S")
        }
    }
}