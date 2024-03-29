package be.florien.anyflow.feature.alarms.list

import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Alarm
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import kotlinx.coroutines.launch
import javax.inject.Inject

class AlarmListViewModel : BaseViewModel() {
    @Inject
    lateinit var alarmsSynchronizer: AlarmsSynchronizer

    val alarmList by lazy {
        alarmsSynchronizer.getAlarms()
    }

    fun repetitionText(alarm: Alarm): List<Int> {
        return when {
            !alarm.isRepeating -> listOf()
            alarm.daysToTrigger.all { it } -> listOf(R.string.weekday_everyday)
            else -> {
                val dayList = mutableListOf<Int>()
                if (alarm.daysToTrigger[0]) dayList.add(R.string.weekday_monday)
                if (alarm.daysToTrigger[1]) dayList.add(R.string.weekday_tuesday)
                if (alarm.daysToTrigger[2]) dayList.add(R.string.weekday_wednesday)
                if (alarm.daysToTrigger[3]) dayList.add(R.string.weekday_thursday)
                if (alarm.daysToTrigger[4]) dayList.add(R.string.weekday_friday)
                if (alarm.daysToTrigger[5]) dayList.add(R.string.weekday_saturday)
                if (alarm.daysToTrigger[6]) dayList.add(R.string.weekday_sunday)
                dayList
            }
        }
    }

    fun setAlarmActive(alarm: Alarm, isActive: Boolean) {
        if (alarm.active != isActive) {
            viewModelScope.launch {
                alarmsSynchronizer.toggleAlarm(alarm)
            }
        }
    }

    fun timeText(alarm: Alarm): String = String.format("%d:%02d", alarm.hour, alarm.minute)
}