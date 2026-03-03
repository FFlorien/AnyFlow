package be.florien.anyflow.feature.alarm.ui.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.feature.alarm.ui.ImmutableAlarm
import be.florien.anyflow.feature.alarm.ui.toViewAlarm
import be.florien.anyflow.management.alarm.AlarmsSynchronizer
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AlarmListState(
    val hasPermission: Boolean,
    val alarms: PersistentList<ImmutableAlarm>
)

class AlarmListViewModel @Inject constructor(val alarmsSynchronizer: AlarmsSynchronizer) :
    BaseViewModel() {

    private val mutableState = MutableStateFlow(AlarmListState(hasPermission(), persistentListOf()))
        .apply {
            viewModelScope.launch {
                alarmsSynchronizer.getAlarms().collect { newAlarms ->
                    update { oldState ->
                        AlarmListState(
                            oldState.hasPermission,
                            newAlarms.map { it.toViewAlarm() }.toPersistentList()
                        )
                    }
                }
            }
        }
    val state = mutableState

    fun setAlarmActive(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmsSynchronizer.toggleAlarm(id)
        }
    }

    fun refreshPermission() {
        viewModelScope.launch(Dispatchers.IO) {
            mutableState.update { AlarmListState(hasPermission(), it.alarms) }
        }
    }

    private fun hasPermission() = alarmsSynchronizer.canScheduleExactAlarms()
}