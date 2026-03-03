package be.florien.anyflow.feature.alarm.ui.edit

import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.feature.alarm.ui.ImmutableAlarm
import be.florien.anyflow.feature.alarm.ui.toViewAlarm
import be.florien.anyflow.management.alarm.AlarmsSynchronizer
import be.florien.anyflow.management.alarm.model.Alarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class EditAlarmViewModel @Inject constructor(val alarmsSynchronizer: AlarmsSynchronizer) :
    BaseViewModel() {

    private val mutableState =
        MutableStateFlow(
            ImmutableAlarm(
                id = 0L,
                hour = 0,
                minute = 0,
                monday = false,
                tuesday = false,
                wednesday = false,
                thursday = false,
                friday = false,
                saturday = false,
                sunday = false,
                active = true
            )
        )

    val state: StateFlow<ImmutableAlarm> = mutableState

    fun setAlarm(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        val newAlarm = alarmsSynchronizer.getAlarms(id)
        mutableState.update { newAlarm.toViewAlarm() }
    }

    fun addAlarm(
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
        viewModelScope.launch(Dispatchers.IO) {
            val isRepeating =
                monday || tuesday || wednesday || thursday || friday || saturday || sunday
            val isEveryday =
                monday && tuesday && wednesday && thursday && friday && saturday && sunday
            when {
                !isRepeating -> alarmsSynchronizer.addSingleAlarm(
                    hour,
                    minute
                )

                isEveryday -> alarmsSynchronizer.addRepeatingAlarm(
                    hour,
                    minute
                )

                else -> alarmsSynchronizer.addRepeatingAlarmForWeekDays(
                    hour = hour,
                    minute = minute,
                    monday = monday,
                    tuesday = tuesday,
                    wednesday = wednesday,
                    thursday = thursday,
                    friday = friday,
                    saturday = saturday,
                    sunday = sunday
                )
            }
        }
    }

    fun editAlarm(
        id: Long,
        isActive: Boolean,
        hours: Int,
        minutes: Int,
        monday: Boolean,
        tuesday: Boolean,
        wednesday: Boolean,
        thursday: Boolean,
        friday: Boolean,
        saturday: Boolean,
        sunday: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newAlarm = Alarm(
                id,
                hours,
                minutes,
                monday || tuesday || wednesday || thursday || friday || saturday || sunday,
                listOf(
                    monday,
                    tuesday,
                    wednesday,
                    thursday,
                    friday,
                    saturday,
                    sunday
                ),
                isActive
            )
            alarmsSynchronizer.updateAlarm(newAlarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmsSynchronizer.deleteAlarm(alarm)
        }
    }
}