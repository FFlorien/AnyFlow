package be.florien.anyflow.feature.alarms

import android.app.AlarmManager
import javax.inject.Inject

class AlarmsSynchronizer @Inject constructor(private val alarmManager: AlarmManager) {
    fun canScheduleExactAlarms() = true // Build.VERSION.SDK_INT >= 31 && alarmManager.canScheduleExactAlarms()
    fun addSingleAlarm(hour: Int, minute: Int) {

    }

    fun addRepeatingAlarm(hour: Int, minute: Int) {

    }

    fun addRepeatingAlarmForWeekDays(hour: Int, minute: Int, monday: Boolean, tuesday: Boolean, wednesday: Boolean, thursday: Boolean, friday: Boolean, saturday: Boolean, sunday: Boolean) {

    }

}